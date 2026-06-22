package co.com.hermes.calendar.scheduling.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Vista del servicio que scheduling necesita del catálogo (leída por el endpoint interno). Solo los
 * campos relevantes para reservar; enums se reciben como texto para no acoplar a catalog.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OfferingSnapshot(
        UUID id,
        UUID tenantId,
        String name,
        int durationMinutes,
        String modality,
        java.math.BigDecimal priceAmount,
        String priceCurrency,
        boolean requiresOnlinePayment,
        boolean active,
        List<Requirement> requirements
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Requirement(String key, String label, String type, boolean required) {
    }
}
