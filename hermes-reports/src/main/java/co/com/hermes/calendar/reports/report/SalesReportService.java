package co.com.hermes.calendar.reports.report;

import co.com.hermes.calendar.reports.client.PaymentReportClient;
import co.com.hermes.calendar.reports.client.PaymentReportClient.SalesReport;
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

/** Ensambla el informe de ventas de un tenant en un periodo. */
@Service
public class SalesReportService {

    private final PaymentReportClient paymentClient;
    private final JasperReportEngine engine;

    public SalesReportService(PaymentReportClient paymentClient, JasperReportEngine engine) {
        this.paymentClient = paymentClient;
        this.engine = engine;
    }

    public byte[] generate(UUID tenantId, LocalDate from, LocalDate to, String generatedAt) {
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC); // [from, to] inclusivo por día
        SalesReport sales = paymentClient.sales(tenantId, fromTs, toTs);

        List<Map<String, ?>> rows = new ArrayList<>();
        for (PaymentReportClient.SalesLine line : sales.lines()) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", ReportFormat.shortDate(line.date()));
            row.put("paymentId", line.paymentId().toString());
            row.put("appointmentId", line.appointmentId() == null ? "-" : line.appointmentId().toString());
            row.put("status", line.status());
            row.put("amount", ReportFormat.money(line.amount()));
            rows.add(row);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TENANT_NAME", tenantId.toString());
        params.put("FROM", ReportFormat.day(from));
        params.put("TO", ReportFormat.day(to));
        params.put("GENERATED_AT", generatedAt);
        params.put("TOTAL_AMOUNT", ReportFormat.money(sales.totalAmount()));
        params.put("CURRENCY", sales.currency());
        params.put("COUNT", String.valueOf(sales.count()));
        return engine.toPdf("sales-report", params, rows);
    }
}
