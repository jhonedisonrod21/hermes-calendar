package co.com.hermes.calendar.scheduling.appointment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Cupo libre para reservar.")
public record AvailableSlot(LocalDateTime start, LocalDateTime end) {
}
