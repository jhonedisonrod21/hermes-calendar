package co.com.hermes.calendar.catalog.search;

import co.com.hermes.calendar.catalog.offering.Modality;
import co.com.hermes.calendar.catalog.offering.Offering;
import co.com.hermes.calendar.catalog.offering.RequirementDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Vista pública de un servicio en la búsqueda (sin campos internos de administración). */
@Schema(description = "Resultado público de búsqueda de un servicio ofertado.")
public record OfferingSearchResult(
        UUID id,
        UUID tenantId,
        String tenantSlug,
        String tenantName,
        String name,
        String description,
        String category,
        int durationMinutes,
        Modality modality,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean requiresOnlinePayment,
        List<RequirementDto> requirements
) {

    public static OfferingSearchResult from(Offering offering) {
        List<RequirementDto> requirements = offering.getRequirements().stream()
                .map(RequirementDto::from)
                .toList();
        return new OfferingSearchResult(
                offering.getId(),
                offering.getTenantId(),
                offering.getTenantSlug(),
                offering.getTenantName(),
                offering.getName(),
                offering.getDescription(),
                offering.getCategory(),
                offering.getDurationMinutes(),
                offering.getModality(),
                offering.getPriceAmount(),
                offering.getPriceCurrency(),
                offering.isRequiresOnlinePayment(),
                requirements
        );
    }
}
