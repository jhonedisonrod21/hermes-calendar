package co.com.hermes.calendar.scheduling.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Emite eventos de cita al servicio de notificaciones (best-effort: si notification no está disponible
 * no se interrumpe la operación de agenda, solo se registra el fallo). Una propagación con garantías
 * (outbox en scheduling) es el endurecimiento posterior.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    public enum AppointmentEventType { CONFIRMED, CANCELLED, RESCHEDULED, EXPIRED }

    private final RestClient notification;
    private final String internalApiKey;

    public NotificationClient(
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${hermes.notification.base-url:http://hermes-notification-service}") String notificationBaseUrl,
            @Value("${hermes.internal.api-key}") String internalApiKey
    ) {
        this.notification = loadBalancedRestClientBuilder.baseUrl(notificationBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public void emitAppointmentEvent(AppointmentEventType type, UUID appointmentId, UUID customerUserId,
                                     String offeringName, LocalDateTime slotStart) {
        Map<String, Object> body = Map.of(
                "type", type.name(),
                "appointmentId", appointmentId,
                "customerUserId", customerUserId,
                "offeringName", offeringName == null ? "" : offeringName,
                "slotStart", slotStart);
        try {
            notification.post()
                    .uri("/internal/notifications/appointment-events")
                    .header(INTERNAL_KEY_HEADER, internalApiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Could not notify appointment event {} for {}: {}", type, appointmentId, ex.getMessage());
        }
    }
}
