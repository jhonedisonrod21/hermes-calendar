package co.com.hermes.calendar.reports.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Lee citas y estadísticas del servicio de agenda por sus endpoints internos (clave compartida). */
@Component
public class SchedulingReportClient {

    private static final String INTERNAL_KEY_HEADER = "X-Hermes-Internal-Key";

    private final RestClient scheduling;
    private final String internalApiKey;

    public SchedulingReportClient(
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${hermes.scheduling.base-url:http://hermes-scheduling-service}") String schedulingBaseUrl,
            @Value("${hermes.internal.api-key}") String internalApiKey) {
        this.scheduling = loadBalancedRestClientBuilder.baseUrl(schedulingBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /** Datos de una cita, o vacío si no existe (404). */
    public Optional<AppointmentInfo> findAppointment(UUID appointmentId) {
        AppointmentInfo info = scheduling.get()
                .uri("/internal/appointments/{id}", appointmentId)
                .header(INTERNAL_KEY_HEADER, internalApiKey)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> { })
                .body(AppointmentInfo.class);
        return Optional.ofNullable(info);
    }

    /** Estadísticas de citas del tenant en [from, to). */
    public AppointmentStats stats(UUID tenantId, LocalDateTime from, LocalDateTime to) {
        AppointmentStats stats = scheduling.get()
                .uri((UriBuilder b) -> b.path("/internal/appointments/stats")
                        .queryParam("tenantId", tenantId)
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .header(INTERNAL_KEY_HEADER, internalApiKey)
                .retrieve()
                .body(AppointmentStats.class);
        if (stats == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sin respuesta del servicio de agenda");
        }
        return stats;
    }

    public record AppointmentInfo(
            UUID id, UUID tenantId, UUID offeringId, UUID customerUserId,
            LocalDateTime slotStart, LocalDateTime slotEnd, String status) {
    }

    public record AppointmentStats(
            UUID tenantId, LocalDateTime from, LocalDateTime to, long total,
            long pendingPayment, long confirmed, long completed,
            long cancelled, long noShow, long expired) {
    }
}
