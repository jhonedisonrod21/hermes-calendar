package co.com.hermes.calendar.reports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de reportes (PDF) basado en plantillas JasperReports alojadas localmente
 * ({@code classpath:reports/*.jrxml}). No tiene base de datos: agrega los datos llamando
 * a los endpoints internos de payment, scheduling y catalog (clave compartida).
 */
@SpringBootApplication
public class ReportsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportsApplication.class, args);
    }
}
