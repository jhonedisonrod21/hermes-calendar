package co.com.hermes.calendar.reports.web;

import co.com.hermes.calendar.reports.report.ReceiptReportService;
import co.com.hermes.calendar.reports.report.SalesReportService;
import co.com.hermes.calendar.reports.report.StatisticsReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Reportes en PDF del establecimiento. Tras el gateway: {@code /reports/**}. El tenant sale del token;
 * reservado a miembros con permisos de calendario (TENANT_ADMIN / TENANT_PARTNER).
 */
@RestController
@RequestMapping
@Tag(name = "Reportes", description = "Generación de reportes en PDF (comprobante, ventas, estadísticas).")
@SecurityRequirement(name = "bearer-jwt")
public class ReportController {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReceiptReportService receiptService;
    private final SalesReportService salesService;
    private final StatisticsReportService statisticsService;

    public ReportController(ReceiptReportService receiptService, SalesReportService salesService,
                            StatisticsReportService statisticsService) {
        this.receiptService = receiptService;
        this.salesService = salesService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/payments/{paymentId}/receipt")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Comprobante de pago (PDF): del dueño del pago (cliente) o de su establecimiento")
    public ResponseEntity<byte[]> paymentReceipt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        byte[] pdf = receiptService.generate(paymentId, callerUserId(jwt), callerTenantOrNull(jwt), now());
        return pdf(pdf, "comprobante-" + paymentId + ".pdf");
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyAuthority('calendar:read','calendar:write')")
    @Operation(summary = "Informe de ventas (PDF) de mi establecimiento en un periodo")
    public ResponseEntity<byte[]> sales(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = range(from, to);
        byte[] pdf = salesService.generate(callerTenant(jwt), range[0], range[1], now());
        return pdf(pdf, "ventas-" + range[0] + "_" + range[1] + ".pdf");
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('calendar:read','calendar:write')")
    @Operation(summary = "Estadísticas (PDF) de mi establecimiento en un periodo")
    public ResponseEntity<byte[]> statistics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = range(from, to);
        byte[] pdf = statisticsService.generate(callerTenant(jwt), range[0], range[1], now());
        return pdf(pdf, "estadisticas-" + range[0] + "_" + range[1] + ".pdf");
    }

    private static ResponseEntity<byte[]> pdf(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }

    /** Rango por defecto: del primer día del mes actual a hoy. */
    private static LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.withDayOfMonth(1);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' no puede ser posterior a 'to'");
        }
        return new LocalDate[]{start, end};
    }

    private static String now() {
        return LocalDateTime.now().format(STAMP);
    }

    private static UUID callerTenant(Jwt jwt) {
        UUID tenantId = callerTenantOrNull(jwt);
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenant context in token");
        }
        return tenantId;
    }

    /** Tenant del token, o {@code null} si el llamante no tiene contexto de organización (p. ej. GUEST_USER). */
    private static UUID callerTenantOrNull(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid tenant context");
        }
    }

    /** Usuario del llamante, del token (claim user_id, con fallback a sub). */
    private static UUID callerUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user context");
        }
    }
}
