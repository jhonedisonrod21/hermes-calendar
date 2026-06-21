package co.com.hermes.calendar.scheduling.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Franja de horario para un día (varias por día = turno partido / almuerzo).")
public record BusinessHoursDto(
        @NotNull @Schema(example = "MONDAY") DayOfWeek dayOfWeek,
        @NotNull @Schema(example = "08:00") LocalTime opensAt,
        @NotNull @Schema(example = "12:00") LocalTime closesAt
) {

    public static BusinessHoursDto from(BusinessHours hours) {
        return new BusinessHoursDto(hours.getDayOfWeek(), hours.getOpensAt(), hours.getClosesAt());
    }
}
