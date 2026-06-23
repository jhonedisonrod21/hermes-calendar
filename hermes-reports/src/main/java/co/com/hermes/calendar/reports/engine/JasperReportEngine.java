package co.com.hermes.calendar.reports.engine;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Compila, cachea y rellena las plantillas JasperReports alojadas en {@code classpath:reports/*.jrxml}
 * y exporta el resultado a PDF. La compilación (vía el compilador JDT empaquetado) ocurre una vez por
 * plantilla y se cachea, así que las peticiones siguientes solo rellenan y exportan.
 */
@Component
public class JasperReportEngine {

    private static final Logger log = LoggerFactory.getLogger(JasperReportEngine.class);

    private final ConcurrentMap<String, JasperReport> compiled = new ConcurrentHashMap<>();

    /** Logo corporativo (PNG en base64) inyectado en todas las plantillas como {@code $P{LOGO_BASE64}}. */
    private volatile String logoBase64;

    /**
     * Genera un PDF a partir de una plantilla.
     *
     * @param template nombre del fichero sin extensión bajo {@code classpath:reports/} (p. ej. "payment-receipt")
     * @param params   parámetros del reporte ({@code $P{...}})
     * @param rows     filas del datasource ({@code $F{...}}); si es nula/vacía se usa un datasource de 1 registro
     *                 vacío (para reportes de un solo registro construidos solo con parámetros).
     */
    public byte[] toPdf(String template, Map<String, Object> params, Collection<Map<String, ?>> rows) {
        JasperReport report = compiled.computeIfAbsent(template, this::compile);
        JRDataSource dataSource = (rows == null || rows.isEmpty())
                ? new JREmptyDataSource()
                : new JRMapCollectionDataSource(List.copyOf(rows));
        try {
            Map<String, Object> merged = new HashMap<>(params == null ? Map.of() : params);
            merged.putIfAbsent("LOGO_BASE64", logo());
            JasperPrint print = JasperFillManager.fillReport(report, merged, dataSource);
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception ex) {
            log.error("Error generando el reporte '{}': {}", template, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el reporte");
        }
    }

    /** Carga (y cachea) el logo en base64 desde el classpath; cadena vacía si no está disponible. */
    private String logo() {
        if (logoBase64 == null) {
            try (InputStream in = new ClassPathResource("reports/logo-base64.txt").getInputStream()) {
                logoBase64 = new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim();
            } catch (Exception ex) {
                log.warn("No se pudo cargar el logo del reporte: {}", ex.getMessage());
                logoBase64 = "";
            }
        }
        return logoBase64;
    }

    private JasperReport compile(String template) {
        try (InputStream in = new ClassPathResource("reports/" + template + ".jrxml").getInputStream()) {
            log.info("Compilando plantilla de reporte '{}'", template);
            return JasperCompileManager.compileReport(in);
        } catch (Exception ex) {
            log.error("No se pudo compilar la plantilla '{}': {}", template, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Plantilla de reporte inválida: " + template);
        }
    }
}
