package co.com.hermes.calendar.reports.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lee pagos del servicio de pagos por sus endpoints internos (clave compartida). */
@Component
public class PaymentReportClient {

    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    private final RestClient payment;
    private final String internalApiKey;

    public PaymentReportClient(
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${hermes.payment.base-url:http://hermes-payment-service}") String paymentBaseUrl,
            @Value("${hermes.internal.api-key}") String internalApiKey) {
        this.payment = loadBalancedRestClientBuilder.baseUrl(paymentBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /** Detalle de un pago, o vacío si no existe (404). */
    public Optional<PaymentDetail> findPayment(UUID paymentId) {
        PaymentDetail detail = payment.get()
                .uri("/internal/payments/{id}", paymentId)
                .header(INTERNAL_KEY_HEADER, internalApiKey)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> { })
                .body(PaymentDetail.class);
        return Optional.ofNullable(detail);
    }

    /** Agregado de ventas cobradas de un tenant en [from, to). */
    public SalesReport sales(UUID tenantId, OffsetDateTime from, OffsetDateTime to) {
        SalesReport report = payment.get()
                .uri((UriBuilder b) -> b.path("/internal/payments/sales")
                        .queryParam("tenantId", tenantId)
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .header(INTERNAL_KEY_HEADER, internalApiKey)
                .retrieve()
                .body(SalesReport.class);
        if (report == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sin respuesta del servicio de pagos");
        }
        return report;
    }

    public record PaymentDetail(
            UUID id, UUID appointmentId, UUID tenantId, UUID customerUserId,
            BigDecimal amount, String currency, String status, String provider,
            String providerReference, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record SalesReport(
            UUID tenantId, OffsetDateTime from, OffsetDateTime to, String currency,
            BigDecimal totalAmount, int count, List<SalesLine> lines) {
    }

    public record SalesLine(
            UUID paymentId, UUID appointmentId, OffsetDateTime date,
            BigDecimal amount, String currency, String status) {
    }
}
