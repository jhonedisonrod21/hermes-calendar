package co.com.hermes.calendar.scheduling.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Solicitud de reserva de cita.")
public record AppointmentBookingRequest(
        @NotNull UUID offeringId,
        @NotNull @Schema(example = "2026-07-01T09:00:00") LocalDateTime slotStart,
        @Schema(description = "Valores de los anexos exigidos por el servicio (clave -> valor).")
        Map<String, String> requirementValues
) {
}
