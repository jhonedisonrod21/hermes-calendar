package co.com.hermes.calendar.scheduling.schedule;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Horario y feriados del establecimiento. Tras el gateway (StripPrefix=1) queda bajo
 * {@code /scheduling/hours} y {@code /scheduling/exceptions}. Gestión del TENANT_ADMIN; el tenant
 * se toma del token.
 */
@RestController
@PreAuthorize("hasRole('TENANT_ADMIN')")
@Tag(name = "Tenant schedule", description = "Horario laboral y feriados/cierres del establecimiento.")
@SecurityRequirement(name = "bearer-jwt")
public class TenantScheduleController {

    private final TenantScheduleService service;

    public TenantScheduleController(TenantScheduleService service) {
        this.service = service;
    }

    @GetMapping("/hours")
    @Operation(summary = "Obtiene el horario semanal")
    public List<BusinessHoursDto> getHours(@AuthenticationPrincipal Jwt jwt) {
        return service.getHours(callerTenant(jwt));
    }

    @PutMapping("/hours")
    @Operation(summary = "Reemplaza el horario semanal completo")
    public List<BusinessHoursDto> replaceHours(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BusinessScheduleRequest request) {
        return service.replaceHours(callerTenant(jwt), request);
    }

    @GetMapping("/exceptions")
    @Operation(summary = "Lista feriados/cierres y horarios especiales")
    public Page<ScheduleExceptionResponse> listExceptions(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return service.listExceptions(callerTenant(jwt), pageable);
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un feriado/cierre u horario especial")
    public ScheduleExceptionResponse addException(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ScheduleExceptionRequest request) {
        return service.addException(callerTenant(jwt), request);
    }

    @DeleteMapping("/exceptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una excepción de calendario")
    public void removeException(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.removeException(callerTenant(jwt), id);
    }

    /** Tenant del llamante, tomado del token (no de input del cliente). */
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
