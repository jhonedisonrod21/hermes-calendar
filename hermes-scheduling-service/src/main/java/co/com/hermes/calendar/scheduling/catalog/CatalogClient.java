package co.com.hermes.calendar.scheduling.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/** Acceso al servicio de catálogo por su endpoint interno (clave compartida). */
@Component
public class CatalogClient {

    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    private final RestClient catalog;
    private final String internalApiKey;

    public CatalogClient(
            RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${hermes.catalog.base-url:http://hermes-catalog-service}") String catalogBaseUrl,
            @Value("${hermes.internal.api-key}") String internalApiKey
    ) {
        this.catalog = loadBalancedRestClientBuilder.baseUrl(catalogBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /** Servicio por id, o vacío si no existe (404). */
    public Optional<OfferingSnapshot> findOffering(UUID offeringId) {
        OfferingSnapshot snapshot = catalog.get()
                .uri("/internal/offerings/{id}", offeringId)
                .header(INTERNAL_KEY_HEADER, internalApiKey)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> { })
                .body(OfferingSnapshot.class);
        return Optional.ofNullable(snapshot);
    }
}
