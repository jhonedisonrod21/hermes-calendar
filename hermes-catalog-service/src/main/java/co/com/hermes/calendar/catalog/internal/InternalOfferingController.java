package co.com.hermes.calendar.catalog.internal;

import co.com.hermes.calendar.catalog.offering.OfferingRepository;
import co.com.hermes.calendar.catalog.offering.OfferingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Lectura interna del catálogo usada por otros servicios del contexto (scheduling) para conocer
 * un servicio (duración, modalidad, precio, pago y requisitos) por su UUID. Protegido por la clave
 * interna compartida; el gateway bloquea /internal/** desde el exterior.
 */
@RestController
@RequestMapping("/internal/offerings")
@Tag(name = "Internal catalog", description = "Lectura interna de servicios por UUID.")
@SecurityRequirement(name = "hermes-internal-key")
public class InternalOfferingController {

    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    private final OfferingRepository offerings;
    private final String internalApiKey;

    public InternalOfferingController(OfferingRepository offerings,
                                      @Value("${hermes.internal.api-key}") String internalApiKey) {
        this.offerings = offerings;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un servicio por id (uso interno)")
    public OfferingResponse get(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Clave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @PathVariable UUID id
    ) {
        assertInternalKey(apiKey);
        return offerings.findById(id)
                .map(OfferingResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found"));
    }

    private void assertInternalKey(String apiKey) {
        if (internalApiKey == null || internalApiKey.isBlank() || apiKey == null
                || !MessageDigest.isEqual(internalApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }
}
