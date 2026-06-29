package co.com.hermes.calendar.scheduling.appointment;

import co.com.hermes.calendar.scheduling.catalog.CatalogClient;
import co.com.hermes.calendar.scheduling.catalog.OfferingSnapshot;
import co.com.hermes.calendar.scheduling.schedule.BusinessHoursRepository;
import co.com.hermes.calendar.scheduling.schedule.ExceptionType;
import co.com.hermes.calendar.scheduling.schedule.ScheduleException;
import co.com.hermes.calendar.scheduling.schedule.ScheduleExceptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calcula los cupos libres de un servicio en una fecha: combina el horario laboral del
 * establecimiento, las excepciones (feriados/cierres/horarios especiales), la duración del servicio
 * y las citas ya reservadas.
 */
@Service
public class AvailabilityService {

    public record OpenInterval(LocalTime opensAt, LocalTime closesAt) { }

    private final CatalogClient catalogClient;
    private final BusinessHoursRepository businessHours;
    private final ScheduleExceptionRepository exceptions;
    private final AppointmentRepository appointments;

    public AvailabilityService(CatalogClient catalogClient, BusinessHoursRepository businessHours,
                               ScheduleExceptionRepository exceptions, AppointmentRepository appointments) {
        this.catalogClient = catalogClient;
        this.businessHours = businessHours;
        this.exceptions = exceptions;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public List<AvailableSlot> availableSlots(UUID offeringId, LocalDate date) {
        OfferingSnapshot offering = requireOffering(offeringId);
        if (!offering.active()) {
            return List.of();
        }
        return slotsFor(offering, date);
    }

    /** Cupos libres para un servicio ya cargado (reutilizado por la reserva). */
    public List<AvailableSlot> slotsFor(OfferingSnapshot offering, LocalDate date) {
        List<OpenInterval> intervals = openIntervals(offering.tenantId(), date);
        if (intervals.isEmpty()) {
            return List.of();
        }
        Set<LocalDateTime> taken = bookedStarts(offering.id(), date);
        int duration = offering.durationMinutes();
        List<AvailableSlot> slots = new ArrayList<>();
        for (OpenInterval interval : intervals) {
            LocalTime cursor = interval.opensAt();
            while (!cursor.plusMinutes(duration).isAfter(interval.closesAt())) {
                LocalDateTime start = date.atTime(cursor);
                if (!taken.contains(start)) {
                    slots.add(new AvailableSlot(start, start.plusMinutes(duration)));
                }
                cursor = cursor.plusMinutes(duration);
            }
        }
        return slots;
    }

    /** Tramos abiertos del establecimiento para una fecha (excepción manda sobre el horario semanal). */
    public List<OpenInterval> openIntervals(UUID tenantId, LocalDate date) {
        ScheduleException exception = exceptions.findByTenantIdAndDate(tenantId, date).orElse(null);
        if (exception != null) {
            if (exception.getType() == ExceptionType.CLOSED) {
                return List.of();
            }
            return List.of(new OpenInterval(exception.getOpensAt(), exception.getClosesAt()));
        }
        return businessHours.findByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId).stream()
                .filter(h -> date.getDayOfWeek().equals(h.getDayOfWeek()))
                .map(h -> new OpenInterval(h.getOpensAt(), h.getClosesAt()))
                .toList();
    }

    private Set<LocalDateTime> bookedStarts(UUID offeringId, LocalDate date) {
        return appointments.findByOfferingIdAndSlotStartGreaterThanEqualAndSlotStartLessThan(
                        offeringId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream()
                .filter(a -> a.getStatus().holdsSlot())
                .map(Appointment::getSlotStart)
                .collect(Collectors.toSet());
    }

    private OfferingSnapshot requireOffering(UUID offeringId) {
        return catalogClient.findOffering(offeringId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found"));
    }
}
