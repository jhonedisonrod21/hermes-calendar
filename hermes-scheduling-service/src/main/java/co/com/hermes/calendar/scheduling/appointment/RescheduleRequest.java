package co.com.hermes.calendar.scheduling.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Nuevo horario para reprogramar una cita.")
public record RescheduleRequest(
        @NotNull @Schema(example = "2026-07-02T10:30:00") LocalDateTime newSlotStart
) {
}
