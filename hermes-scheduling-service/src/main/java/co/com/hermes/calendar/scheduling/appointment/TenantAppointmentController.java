package co.com.hermes.calendar.scheduling.appointment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Citas vistas por el personal del establecimiento. Tras el gateway: {@code /scheduling/me/appointments}.
 * El tenant sale del token; reservado a TENANT_ADMIN / TENANT_PARTNER.
 */
@RestController
@RequestMapping("/me/appointments")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_PARTNER')")
@Tag(name = "Tenant appointments", description = "Citas del establecimiento (personal).")
@SecurityRequirement(name = "bearer-jwt")
public class TenantAppointmentController {

    private final BookingService booking;

    public TenantAppointmentController(BookingService booking) {
        this.booking = booking;
    }

    @GetMapping
    @Operation(summary = "Lista las citas de mi establecimiento")
    public Page<AppointmentResponse> list(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return booking.listForTenant(callerTenant(jwt), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una cita de mi establecimiento")
    public AppointmentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return booking.getForTenant(id, callerTenant(jwt));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela una cita de mi establecimiento")
    public AppointmentResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return booking.cancelByTenant(id, callerTenant(jwt));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reprograma una cita de mi establecimiento")
    public AppointmentResponse reschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @Valid @RequestBody RescheduleRequest request) {
        return booking.rescheduleByTenant(id, callerTenant(jwt), request.newSlotStart());
    }

    private static UUID callerTenant(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenant context in token");
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid tenant context");
        }
    }
}
