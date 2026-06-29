package co.com.hermes.calendar.scheduling.appointment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reserva de citas por el cliente. Tras el gateway: {@code /scheduling/...}. Accesible a cualquier
 * usuario autenticado (incluido GUEST_USER). El tenant de la cita sale del servicio, no del token.
 */
@RestController
@Tag(name = "Appointment booking", description = "Disponibilidad y reserva de citas (cliente).")
@SecurityRequirement(name = "bearer-jwt")
public class AppointmentController {

    private final AvailabilityService availability;
    private final BookingService booking;

    public AppointmentController(AvailabilityService availability, BookingService booking) {
        this.availability = availability;
        this.booking = booking;
    }

    @GetMapping("/offerings/{offeringId}/availability")
    @Operation(summary = "Cupos disponibles de un servicio en una fecha")
    public List<AvailableSlot> availability(
            @PathVariable UUID offeringId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return availability.availableSlots(offeringId, date);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserva una cita")
    public AppointmentResponse book(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AppointmentBookingRequest request) {
        return booking.book(callerUserId(jwt), request);
    }

    @GetMapping("/appointments")
    @Operation(summary = "Lista mis citas")
    public org.springframework.data.domain.Page<AppointmentResponse> myAppointments(
            @AuthenticationPrincipal Jwt jwt, org.springframework.data.domain.Pageable pageable) {
        return booking.listForCustomer(callerUserId(jwt), pageable);
    }

    @GetMapping("/appointments/{id}")
    @Operation(summary = "Consulta una cita propia")
    public AppointmentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return booking.getForCustomer(id, callerUserId(jwt));
    }

    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancela una cita propia")
    public AppointmentResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return booking.cancelByCustomer(id, callerUserId(jwt));
    }

    @PostMapping("/appointments/{id}/reschedule")
    @Operation(summary = "Reprograma una cita propia a un nuevo horario")
    public AppointmentResponse reschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @Valid @RequestBody RescheduleRequest request) {
        return booking.rescheduleByCustomer(id, callerUserId(jwt), request.newSlotStart());
    }

    /** Usuario del llamante, del token (claim user_id, con fallback a sub). */
    private static UUID callerUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user identity");
        }
    }
}
