package co.com.hermes.calendar.catalog.offering;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Vista de un servicio del catálogo.")
public record OfferingResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        String category,
        int durationMinutes,
        Modality modality,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean requiresOnlinePayment,
        boolean active,
        List<RequirementDto> requirements,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static OfferingResponse from(Offering offering) {
        List<RequirementDto> requirements = offering.getRequirements().stream()
                .map(RequirementDto::from)
                .toList();
        return new OfferingResponse(
                offering.getId(),
                offering.getTenantId(),
                offering.getName(),
                offering.getDescription(),
                offering.getCategory(),
                offering.getDurationMinutes(),
                offering.getModality(),
                offering.getPriceAmount(),
                offering.getPriceCurrency(),
                offering.isRequiresOnlinePayment(),
                offering.isActive(),
                requirements,
                offering.getCreatedAt(),
                offering.getUpdatedAt()
        );
    }
}
