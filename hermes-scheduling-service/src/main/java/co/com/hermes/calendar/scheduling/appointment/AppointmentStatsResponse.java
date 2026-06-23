package co.com.hermes.calendar.scheduling.appointment;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Estadísticas de citas de un tenant en un periodo (conteo por estado), para el reporte de estadísticas. */
public record AppointmentStatsResponse(
        UUID tenantId,
        LocalDateTime from,
        LocalDateTime to,
        long total,
        long pendingPayment,
        long confirmed,
        long completed,
        long cancelled,
        long noShow,
        long expired
) {
    public static AppointmentStatsResponse of(UUID tenantId, LocalDateTime from, LocalDateTime to,
                                              List<AppointmentRepository.StatusCount> counts) {
        Map<AppointmentStatus, Long> byStatus = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentRepository.StatusCount c : counts) {
            byStatus.put(c.getStatus(), c.getTotal());
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new AppointmentStatsResponse(
                tenantId, from, to, total,
                byStatus.getOrDefault(AppointmentStatus.PENDING_PAYMENT, 0L),
                byStatus.getOrDefault(AppointmentStatus.CONFIRMED, 0L),
                byStatus.getOrDefault(AppointmentStatus.COMPLETED, 0L),
                byStatus.getOrDefault(AppointmentStatus.CANCELLED, 0L),
                byStatus.getOrDefault(AppointmentStatus.NO_SHOW, 0L),
                byStatus.getOrDefault(AppointmentStatus.EXPIRED, 0L));
    }
}
