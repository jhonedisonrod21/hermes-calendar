package co.com.hermes.calendar.reports.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI reportsServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/reports").description("Via API Gateway"),
                        new Server().url("/").description("Acceso directo al servicio")))
                .info(new Info()
                        .title("Hermes Reports Service API")
                        .version("0.0.1")
                        .description("Genera reportes en PDF a partir de plantillas JasperReports locales: "
                                + "comprobante de pago, informe de ventas y estadísticas del establecimiento. "
                                + "El tenant se toma del JWT."))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
