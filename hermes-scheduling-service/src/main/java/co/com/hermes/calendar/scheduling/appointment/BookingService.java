package co.com.hermes.calendar.scheduling.appointment;

import co.com.hermes.calendar.scheduling.catalog.CatalogClient;
import co.com.hermes.calendar.scheduling.catalog.OfferingSnapshot;
import co.com.hermes.calendar.scheduling.notification.NotificationClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reserva y gestión de citas. El tenant proviene del servicio elegido (reserva cross-tenant: un
 * invitado reserva en un establecimiento ajeno), no del token del cliente.
 */
@Service
public class BookingService {

    private static final List<AppointmentStatus> ACTIVE = List.of(AppointmentStatus.PENDING_PAYMENT, AppointmentStatus.CONFIRMED);

    private final CatalogClient catalogClient;
    private final AvailabilityService availability;
    private final AppointmentRepository appointments;
    private final NotificationClient notificationClient;

    public BookingService(CatalogClient catalogClient, AvailabilityService availability,
                          AppointmentRepository appointments, NotificationClient notificationClient) {
        this.catalogClient = catalogClient;
        this.availability = availability;
        this.appointments = appointments;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public AppointmentResponse book(UUID customerUserId, AppointmentBookingRequest request) {
        OfferingSnapshot offering = catalogClient.findOffering(request.offeringId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found"));
        if (!offering.active()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offering is not active");
        }

        LocalDateTime slotStart = request.slotStart();
        boolean offered = availability.slotsFor(offering, slotStart.toLocalDate()).stream()
                .anyMatch(s -> s.start().equals(slotStart));
        if (!offered) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected slot is not available");
        }

        List<AppointmentRequirementValue> values = collectRequirementValues(offering, request.requirementValues());

        AppointmentStatus status = offering.requiresOnlinePayment()
                ? AppointmentStatus.PENDING_PAYMENT
                : AppointmentStatus.CONFIRMED;

        Appointment appointment = Appointment.book(
                UUID.randomUUID(), offering.tenantId(), offering.id(), customerUserId,
                slotStart, slotStart.plusMinutes(offering.durationMinutes()), status,
                offering.priceAmount(), offering.priceCurrency(), offering.requiresOnlinePayment(), values);
        try {
            Appointment saved = appointments.saveAndFlush(appointment);
            if (saved.getStatus() == AppointmentStatus.CONFIRMED) {
                // Reserva gratuita: queda confirmada al instante, se notifica de inmediato.
                notificationClient.emitAppointmentEvent(NotificationClient.AppointmentEventType.CONFIRMED,
                        saved.getId(), saved.getCustomerUserId(), offering.name(), saved.getSlotStart());
            }
            return AppointmentResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Carrera por el mismo cupo: el índice único de cupo activo lo impide.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot was just taken");
        }
    }

    @Transactional
    public AppointmentResponse rescheduleByCustomer(UUID id, UUID customerUserId, LocalDateTime newSlotStart) {
        Appointment appointment = appointments.findByIdAndCustomerUserId(id, customerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return reschedule(appointment, newSlotStart);
    }

    @Transactional
    public AppointmentResponse rescheduleByTenant(UUID id, UUID tenantId, LocalDateTime newSlotStart) {
        Appointment appointment = appointments.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return reschedule(appointment, newSlotStart);
    }

    private AppointmentResponse reschedule(Appointment appointment, LocalDateTime newSlotStart) {
        if (!appointment.getStatus().holdsSlot()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment cannot be rescheduled in its current state");
        }
        if (appointment.getSlotStart().equals(newSlotStart)) {
            return AppointmentResponse.from(appointment);
        }
        OfferingSnapshot offering = catalogClient.findOffering(appointment.getOfferingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found"));
        boolean available = availability.slotsFor(offering, newSlotStart.toLocalDate()).stream()
                .anyMatch(s -> s.start().equals(newSlotStart));
        if (!available) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected slot is not available");
        }
        appointment.reschedule(newSlotStart, newSlotStart.plusMinutes(offering.durationMinutes()));
        try {
            Appointment saved = appointments.saveAndFlush(appointment);
            notificationClient.emitAppointmentEvent(NotificationClient.AppointmentEventType.RESCHEDULED,
                    saved.getId(), saved.getCustomerUserId(), offering.name(), saved.getSlotStart());
            return AppointmentResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot was just taken");
        }
    }

    /** Resultado de un intento de confirmación de pago (lo usa el endpoint interno para fijar el HTTP status). */
    public enum ConfirmationResult { CONFIRMED, ALREADY_CONFIRMED, NOT_CONFIRMABLE }

    @Transactional(readOnly = true)
    public AppointmentSnapshot snapshot(UUID id) {
        return AppointmentSnapshot.from(appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found")));
    }

    /**
     * Confirma una cita tras oficializarse su pago (uso interno, idempotente). PENDING_PAYMENT pasa a
     * CONFIRMED; si ya estaba CONFIRMED es un no-op; cualquier otro estado (p. ej. expirada o cancelada)
     * no es confirmable y el llamante debe desistir (eventual reembolso, fuera de alcance del MVP).
     */
    @Transactional
    public ConfirmationResult confirmPayment(UUID id) {
        Appointment appointment = appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return switch (appointment.getStatus()) {
            case PENDING_PAYMENT -> {
                appointment.changeStatus(AppointmentStatus.CONFIRMED);
                notificationClient.emitAppointmentEvent(NotificationClient.AppointmentEventType.CONFIRMED,
                        appointment.getId(), appointment.getCustomerUserId(), null, appointment.getSlotStart());
                yield ConfirmationResult.CONFIRMED;
            }
            case CONFIRMED -> ConfirmationResult.ALREADY_CONFIRMED;
            default -> ConfirmationResult.NOT_CONFIRMABLE;
        };
    }

    /**
     * Expira las reservas que llevan demasiado tiempo esperando el pago. Al pasar a EXPIRED la columna
     * generada de cupo activo queda NULL y el horario vuelve a estar disponible. Idempotente: solo toca
     * citas aún en PENDING_PAYMENT, así que es seguro ante múltiples instancias.
     */
    @Transactional
    public int expireStalePendingPayments(Duration ttl) {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(ttl);
        List<Appointment> stale = appointments.findByStatusAndCreatedAtBefore(AppointmentStatus.PENDING_PAYMENT, cutoff);
        stale.forEach(a -> {
            a.changeStatus(AppointmentStatus.EXPIRED);
            // Avisar al cliente que perdió el cupo por falta de pago (best-effort).
            notificationClient.emitAppointmentEvent(NotificationClient.AppointmentEventType.EXPIRED,
                    a.getId(), a.getCustomerUserId(), null, a.getSlotStart());
        });
        return stale.size();
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> listForCustomer(UUID customerUserId, Pageable pageable) {
        return appointments.findByCustomerUserId(customerUserId, pageable).map(AppointmentResponse::from);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getForCustomer(UUID id, UUID customerUserId) {
        return AppointmentResponse.from(appointments.findByIdAndCustomerUserId(id, customerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found")));
    }

    @Transactional
    public AppointmentResponse cancelByCustomer(UUID id, UUID customerUserId) {
        Appointment appointment = appointments.findByIdAndCustomerUserId(id, customerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return cancel(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> listForTenant(UUID tenantId, Pageable pageable) {
        return appointments.findByTenantId(tenantId, pageable).map(AppointmentResponse::from);
    }

    /** Estadísticas de citas del tenant en un rango de fechas de cita [from, to) (uso interno: reportes). */
    @Transactional(readOnly = true)
    public AppointmentStatsResponse statsForTenant(UUID tenantId, LocalDateTime from, LocalDateTime to) {
        return AppointmentStatsResponse.of(tenantId, from, to, appointments.countByStatus(tenantId, from, to));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getForTenant(UUID id, UUID tenantId) {
        return AppointmentResponse.from(appointments.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found")));
    }

    @Transactional
    public AppointmentResponse cancelByTenant(UUID id, UUID tenantId) {
        Appointment appointment = appointments.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return cancel(appointment);
    }

    private AppointmentResponse cancel(Appointment appointment) {
        if (!appointment.getStatus().holdsSlot()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment cannot be cancelled in its current state");
        }
        appointment.changeStatus(AppointmentStatus.CANCELLED);
        notificationClient.emitAppointmentEvent(NotificationClient.AppointmentEventType.CANCELLED,
                appointment.getId(), appointment.getCustomerUserId(), null, appointment.getSlotStart());
        return AppointmentResponse.from(appointment);
    }

    /** El establecimiento da por atendida la cita: CONFIRMED -> COMPLETED. */
    @Transactional
    public AppointmentResponse completeByTenant(UUID id, UUID tenantId) {
        Appointment appointment = appointments.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return closeAs(appointment, AppointmentStatus.COMPLETED);
    }

    /** El establecimiento marca que el cliente no se presentó: CONFIRMED -> NO_SHOW. */
    @Transactional
    public AppointmentResponse markNoShowByTenant(UUID id, UUID tenantId) {
        Appointment appointment = appointments.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return closeAs(appointment, AppointmentStatus.NO_SHOW);
    }

    /**
     * Cierra una cita en un estado terminal (COMPLETED / NO_SHOW). Solo es válido desde CONFIRMED:
     * una cita pendiente de pago, cancelada o expirada no puede cerrarse así. Idempotente cuando ya
     * está en el estado destino (tolera reintentos del personal); cualquier otra transición es 409.
     * Al dejar de retener el cupo (holdsSlot=false), la columna generada de cupo activo queda NULL.
     */
    private AppointmentResponse closeAs(Appointment appointment, AppointmentStatus target) {
        AppointmentStatus current = appointment.getStatus();
        if (current == target) {
            return AppointmentResponse.from(appointment);
        }
        if (current != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only a CONFIRMED appointment can be marked as " + target);
        }
        appointment.changeStatus(target);
        return AppointmentResponse.from(appointment);
    }

    private static List<AppointmentRequirementValue> collectRequirementValues(OfferingSnapshot offering, Map<String, String> provided) {
        Map<String, String> values = provided == null ? Map.of() : provided;
        List<OfferingSnapshot.Requirement> requirements = offering.requirements() == null ? List.of() : offering.requirements();
        List<AppointmentRequirementValue> result = new ArrayList<>();
        for (OfferingSnapshot.Requirement req : requirements) {
            String value = values.get(req.key());
            boolean blank = value == null || value.isBlank();
            if (req.required() && blank) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required input: " + req.key());
            }
            if (!blank) {
                result.add(new AppointmentRequirementValue(UUID.randomUUID(), req.key(), value.trim()));
            }
        }
        return result;
    }
}
