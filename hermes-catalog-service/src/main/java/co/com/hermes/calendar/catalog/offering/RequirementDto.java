package co.com.hermes.calendar.catalog.offering;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Anexo/dato exigido al reservar el servicio.")
public record RequirementDto(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[a-zA-Z0-9_]+", message = "key must be alphanumeric/underscore")
        @Schema(example = "vehicle_plate") String key,
        @NotBlank @Size(max = 160) @Schema(example = "Matricula del vehiculo") String label,
        @NotNull @Schema(example = "TEXT") RequirementType type,
        @Schema(example = "true") boolean required,
        @PositiveOrZero int displayOrder
) {

    public static RequirementDto from(OfferingRequirement requirement) {
        return new RequirementDto(
                requirement.getKey(),
                requirement.getLabel(),
                requirement.getType(),
                requirement.isRequired(),
                requirement.getDisplayOrder()
        );
    }
}
