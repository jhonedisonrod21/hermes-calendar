package co.com.hermes.calendar.catalog.offering;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Datos de un servicio ofertado (alta y edición).")
public record OfferingRequest(
        @NotNull @Size(max = 160) @Schema(example = "Cita de odontologia general") String name,
        @Size(max = 1000) String description,
        @Size(max = 80) @Schema(example = "Odontologia") String category,
        @Positive @Schema(example = "30") int durationMinutes,
        @NotNull @Schema(example = "IN_PERSON") Modality modality,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) @Schema(example = "80000.00") BigDecimal priceAmount,
        @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be an ISO-4217 code") @Schema(example = "COP") String priceCurrency,
        @Schema(example = "false") boolean requiresOnlinePayment,
        @Valid List<RequirementDto> requirements
) {
}
