package co.com.hermes.calendar.scheduling.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Horario semanal completo (reemplaza el existente).")
public record BusinessScheduleRequest(
        @NotNull @Valid List<BusinessHoursDto> hours
) {
}
