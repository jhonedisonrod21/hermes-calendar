package co.com.hermes.calendar.scheduling.appointment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Acceso interno a las citas usado por el servicio de pagos: leer el importe a cobrar y confirmar la
 * cita cuando el pago se oficializa. Protegido por la clave interna compartida; el gateway bloquea
 * /internal/** desde el exterior.
 */
@RestController
@RequestMapping("/internal/appointments")
@Tag(name = "Internal appointments", description = "Lectura y confirmación interna de citas.")
@SecurityRequirement(name = "hermes-internal-key")
public class InternalAppointmentController {

    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    private final BookingService booking;
    private final String internalApiKey;

    public InternalAppointmentController(BookingService booking,
                                         @Value("${hermes.internal.api-key}") String internalApiKey) {
        this.booking = booking;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una cita por id (uso interno)")
    public AppointmentSnapshot get(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Clave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @PathVariable UUID id
    ) {
        assertInternalKey(apiKey);
        return booking.snapshot(id);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirma una cita tras oficializarse el pago (idempotente)")
    public ResponseEntity<Void> confirm(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Clave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @PathVariable UUID id
    ) {
        assertInternalKey(apiKey);
        BookingService.ConfirmationResult result = booking.confirmPayment(id);
        return switch (result) {
            case CONFIRMED, ALREADY_CONFIRMED -> ResponseEntity.ok().build();
            // La cita ya no puede confirmarse (p. ej. expirada): el llamante debe desistir, no reintentar.
            case NOT_CONFIRMABLE -> ResponseEntity.status(HttpStatus.CONFLICT).build();
        };
    }

    private void assertInternalKey(String apiKey) {
        if (internalApiKey == null || internalApiKey.isBlank() || apiKey == null
                || !MessageDigest.isEqual(internalApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }
}
