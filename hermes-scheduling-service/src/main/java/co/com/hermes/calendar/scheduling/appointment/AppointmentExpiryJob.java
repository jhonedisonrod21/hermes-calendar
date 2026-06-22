package co.com.hermes.calendar.scheduling.appointment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Libera los cupos de las reservas que nunca se pagaron. Una cita queda en PENDING_PAYMENT al
 * reservar un servicio de pago en línea; si transcurre el TTL sin que el pago se oficialice, pasa a
 * EXPIRED y su horario vuelve a estar disponible. La operación es idempotente, por lo que correr en
 * varias instancias no causa daño (no se requiere ShedLock para esta tarea).
 */
@Component
public class AppointmentExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(AppointmentExpiryJob.class);

    private final BookingService booking;
    private final Duration holdTtl;

    public AppointmentExpiryJob(BookingService booking,
                                @Value("${hermes.scheduling.payment-hold-ttl:15m}") Duration holdTtl) {
        this.booking = booking;
        this.holdTtl = holdTtl;
    }

    @Scheduled(
            fixedDelayString = "${hermes.scheduling.expiry-job.fixed-delay:60s}",
            initialDelayString = "${hermes.scheduling.expiry-job.initial-delay:30s}")
    public void expireStaleHolds() {
        int expired = booking.expireStalePendingPayments(holdTtl);
        if (expired > 0) {
            log.info("Expired {} appointment(s) past the {} payment hold", expired, holdTtl);
        }
    }
}
