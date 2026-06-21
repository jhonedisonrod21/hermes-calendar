package co.com.hermes.calendar.scheduling.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Excepción de calendario (feriado/cierre u horario especial).")
public record ScheduleExceptionRequest(
        @NotNull @Schema(example = "2026-12-25") LocalDate date,
        @NotNull @Schema(example = "CLOSED") ExceptionType type,
        @Schema(example = "09:00") LocalTime opensAt,
        @Schema(example = "13:00") LocalTime closesAt,
        @Size(max = 200) @Schema(example = "Navidad") String description
) {
}
