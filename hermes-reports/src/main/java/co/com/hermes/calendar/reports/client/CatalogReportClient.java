package co.com.hermes.calendar.reports.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/** Lee el catálogo por su endpoint interno (clave compartida); solo se usa el nombre del servicio. */
@Component
public class CatalogReportClient {

    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    private final RestClient catalog;
    private final String internalApiKey;

    public CatalogReportClient(
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${hermes.catalog.base-url:http://hermes-catalog-service}") String catalogBaseUrl,
            @Value("${hermes.internal.api-key}") String internalApiKey) {
        this.catalog = loadBalancedRestClientBuilder.baseUrl(catalogBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /** Nombre del servicio (offering), o vacío si no se pudo resolver (best-effort). */
    public Optional<String> offeringName(UUID offeringId) {
        try {
            Offering offering = catalog.get()
                    .uri("/internal/offerings/{id}", offeringId)
                    .header(INTERNAL_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> { })
                    .body(Offering.class);
            return Optional.ofNullable(offering).map(Offering::name);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Offering(UUID id, String name) {
    }
}
