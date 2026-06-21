package co.com.hermes.calendar.catalog.offering;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Activa o desactiva un servicio del catálogo.")
public record OfferingActiveRequest(
        @NotNull @Schema(example = "false") Boolean active
) {
}
