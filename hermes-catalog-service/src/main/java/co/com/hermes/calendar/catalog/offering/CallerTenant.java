package co.com.hermes.calendar.catalog.offering;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Identidad de tenant del llamante, tomada del token (claims {@code tenant_id/tenant_slug/tenant_name}),
 * nunca de input del cliente. {@code slug}/{@code name} se denormalizan en el catálogo para la búsqueda.
 */
public record CallerTenant(UUID id, String slug, String name) {

    public static CallerTenant from(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenant context in token");
        }
        try {
            return new CallerTenant(
                    UUID.fromString(tenantId),
                    jwt.getClaimAsString("tenant_slug"),
                    jwt.getClaimAsString("tenant_name"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid tenant context");
        }
    }
}
