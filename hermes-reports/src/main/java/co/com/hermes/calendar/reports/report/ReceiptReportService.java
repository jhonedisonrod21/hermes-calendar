package co.com.hermes.calendar.reports.report;

import co.com.hermes.calendar.reports.client.CatalogReportClient;
import co.com.hermes.calendar.reports.client.PaymentReportClient;
import co.com.hermes.calendar.reports.client.PaymentReportClient.PaymentDetail;
import co.com.hermes.calendar.reports.client.SchedulingReportClient;
import co.com.hermes.calendar.reports.engine.JasperReportEngine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ensambla el comprobante de pago: detalle del pago + cita + nombre del servicio. */
@Service
public class ReceiptReportService {

    private final PaymentReportClient paymentClient;
    private final SchedulingReportClient schedulingClient;
    private final CatalogReportClient catalogClient;
    private final JasperReportEngine engine;

    public ReceiptReportService(PaymentReportClient paymentClient, SchedulingReportClient schedulingClient,
                                CatalogReportClient catalogClient, JasperReportEngine engine) {
        this.paymentClient = paymentClient;
        this.schedulingClient = schedulingClient;
        this.catalogClient = catalogClient;
        this.engine = engine;
    }

    /** Genera el PDF del comprobante de un pago, validando que pertenezca al tenant que lo solicita. */
    public byte[] generate(UUID paymentId, UUID callerTenantId, String generatedAt) {
        PaymentDetail payment = paymentClient.findPayment(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        // Anti-IDOR: el pago debe ser del establecimiento que pide el comprobante.
        if (!callerTenantId.equals(payment.tenantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
        }

        // Enriquecimiento best-effort: fecha de la cita y nombre del servicio.
        String slot = "-";
        String offeringName = "-";
        if (payment.appointmentId() != null) {
            var appointment = schedulingClient.findAppointment(payment.appointmentId());
            if (appointment.isPresent()) {
                LocalDateTime start = appointment.get().slotStart();
                slot = ReportFormat.dateTime(start);
                offeringName = catalogClient.offeringName(appointment.get().offeringId()).orElse("-");
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TENANT_NAME", payment.tenantId().toString());
        params.put("PAYMENT_ID", payment.id().toString());
        params.put("PAYMENT_DATE", ReportFormat.date(payment.createdAt()));
        params.put("STATUS", mapStatus(payment.status()));
        params.put("OFFERING_NAME", offeringName);
        params.put("SLOT", slot);
        params.put("CUSTOMER", payment.customerUserId() == null ? "-" : payment.customerUserId().toString());
        params.put("PROVIDER", payment.provider() == null ? "-" : payment.provider());
        params.put("PROVIDER_REF", payment.providerReference() == null ? "-" : payment.providerReference());
        params.put("AMOUNT", ReportFormat.money(payment.amount()));
        params.put("CURRENCY", payment.currency());
        params.put("GENERATED_AT", generatedAt);
        return engine.toPdf("payment-receipt", params, null);
    }

    private static String mapStatus(String status) {
        return switch (status) {
            case "PAID" -> "PAGADO";
            case "PENDING" -> "PENDIENTE";
            case "FAILED" -> "FALLIDO";
            case "EXPIRED" -> "EXPIRADO";
            case "CANCELLED" -> "ANULADO";
            default -> status;
        };
    }
}
