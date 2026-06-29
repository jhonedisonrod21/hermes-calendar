package co.com.hermes.calendar.reports.report;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formateo de importes y fechas para los reportes (estilo es-CO: miles con '.', decimales con ','). */
final class ReportFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.of("es", "CO"));
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.of("es", "CO"));

    private ReportFormat() {
    }

    static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(amount == null ? BigDecimal.ZERO : amount);
    }

    static String date(OffsetDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME);
    }

    static String dateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME);
    }

    static String day(java.time.LocalDate value) {
        return value == null ? "-" : value.format(DATE);
    }

    static String shortDate(OffsetDateTime value) {
        return value == null ? "-" : value.format(DATE);
    }
}
