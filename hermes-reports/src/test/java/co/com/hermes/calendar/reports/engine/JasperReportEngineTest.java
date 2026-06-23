package co.com.hermes.calendar.reports.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Compila, rellena y exporta a PDF las tres plantillas (valida el JRXML de JasperReports 7 de extremo a extremo). */
class JasperReportEngineTest {

    private final JasperReportEngine engine = new JasperReportEngine();

    @Test
    void rendersPaymentReceipt() {
        byte[] pdf = engine.toPdf("payment-receipt", Map.of(
                "TENANT_NAME", "Clinica Demo",
                "PAYMENT_ID", "p-123",
                "PAYMENT_DATE", "2026-06-23 10:00",
                "STATUS", "PAGADO",
                "OFFERING_NAME", "Consulta general",
                "SLOT", "2026-06-25 09:00",
                "CUSTOMER", "u-9",
                "PROVIDER", "PSE",
                "AMOUNT", "50.000,00"), null);
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void rendersSalesReport() {
        List<Map<String, ?>> rows = List.of(
                Map.of("date", "2026-06-20", "paymentId", "p-1", "appointmentId", "a-1", "status", "PAID", "amount", "30.000,00"),
                Map.of("date", "2026-06-21", "paymentId", "p-2", "appointmentId", "a-2", "status", "PAID", "amount", "45.000,00"));
        byte[] pdf = engine.toPdf("sales-report", Map.of(
                "TENANT_NAME", "Clinica Demo", "FROM", "2026-06-01", "TO", "2026-06-30",
                "GENERATED_AT", "2026-06-23", "TOTAL_AMOUNT", "75.000,00", "CURRENCY", "COP", "COUNT", "2"), rows);
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void rendersStatistics() {
        List<Map<String, ?>> rows = List.of(
                Map.of("label", "Completadas", "count", "12"),
                Map.of("label", "Canceladas", "count", "3"));
        byte[] pdf = engine.toPdf("tenant-statistics", Map.of(
                "TENANT_NAME", "Clinica Demo", "FROM", "2026-06-01", "TO", "2026-06-30",
                "GENERATED_AT", "2026-06-23", "TOTAL_APPOINTMENTS", "15", "COMPLETION_RATE", "80%",
                "REVENUE_AMOUNT", "75.000,00", "CURRENCY", "COP"), rows);
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
