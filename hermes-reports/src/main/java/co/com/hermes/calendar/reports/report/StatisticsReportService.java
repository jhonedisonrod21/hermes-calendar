package co.com.hermes.calendar.reports.report;

import co.com.hermes.calendar.reports.client.PaymentReportClient;
import co.com.hermes.calendar.reports.client.PaymentReportClient.SalesReport;
import co.com.hermes.calendar.reports.client.SchedulingReportClient;
import co.com.hermes.calendar.reports.client.SchedulingReportClient.AppointmentStats;
import co.com.hermes.calendar.reports.engine.JasperReportEngine;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ensambla las estadísticas del establecimiento: citas por estado (agenda) + ingresos (pagos). */
@Service
public class StatisticsReportService {

    private final SchedulingReportClient schedulingClient;
    private final PaymentReportClient paymentClient;
    private final JasperReportEngine engine;

    public StatisticsReportService(SchedulingReportClient schedulingClient, PaymentReportClient paymentClient,
                                   JasperReportEngine engine) {
        this.schedulingClient = schedulingClient;
        this.paymentClient = paymentClient;
        this.engine = engine;
    }

    public byte[] generate(UUID tenantId, LocalDate from, LocalDate to, String generatedAt) {
        AppointmentStats stats = schedulingClient.stats(
                tenantId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        SalesReport sales = paymentClient.sales(tenantId, fromTs, toTs);

        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(statusRow("Pendientes de pago", stats.pendingPayment()));
        rows.add(statusRow("Confirmadas", stats.confirmed()));
        rows.add(statusRow("Finalizadas", stats.completed()));
        rows.add(statusRow("Canceladas", stats.cancelled()));
        rows.add(statusRow("No presentado", stats.noShow()));
        rows.add(statusRow("Expiradas", stats.expired()));

        long finished = stats.completed();
        long attended = stats.completed() + stats.noShow();
        String completionRate = attended == 0 ? "-" : Math.round(100.0 * finished / attended) + "%";

        Map<String, Object> params = new HashMap<>();
        params.put("TENANT_NAME", tenantId.toString());
        params.put("FROM", ReportFormat.day(from));
        params.put("TO", ReportFormat.day(to));
        params.put("GENERATED_AT", generatedAt);
        params.put("TOTAL_APPOINTMENTS", String.valueOf(stats.total()));
        params.put("COMPLETION_RATE", completionRate);
        params.put("REVENUE_AMOUNT", ReportFormat.money(sales.totalAmount()));
        params.put("CURRENCY", sales.currency());
        return engine.toPdf("tenant-statistics", params, rows);
    }

    private static Map<String, Object> statusRow(String label, long count) {
        Map<String, Object> row = new HashMap<>();
        row.put("label", label);
        row.put("count", String.valueOf(count));
        return row;
    }
}
