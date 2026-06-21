package co.com.hermes.calendar.catalog.offering;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Catálogo de servicios del establecimiento. Tras el gateway (StripPrefix=1) queda bajo
 * {@code /catalog/offerings}. El tenant se toma del token; gestión reservada al TENANT_ADMIN.
 */
@RestController
@RequestMapping("/offerings")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@Tag(name = "Catalog offerings", description = "Servicios ofertados por el establecimiento.")
@SecurityRequirement(name = "bearer-jwt")
public class OfferingAdminController {

    private final OfferingAdminService service;

    public OfferingAdminController(OfferingAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un servicio ofertado")
    public OfferingResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OfferingRequest request) {
        return service.create(CallerTenant.from(jwt), request);
    }

    @GetMapping
    @Operation(summary = "Lista los servicios del catálogo")
    public Page<OfferingResponse> list(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return service.list(CallerTenant.from(jwt).id(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un servicio")
    public OfferingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(CallerTenant.from(jwt).id(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica un servicio")
    public OfferingResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody OfferingRequest request) {
        return service.update(CallerTenant.from(jwt), id, request);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activa o desactiva un servicio")
    public OfferingResponse changeActive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody OfferingActiveRequest request) {
        return service.changeActive(CallerTenant.from(jwt).id(), id, request.active());
    }
}
