package co.com.hermes.calendar.scheduling.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Builders de RestClient. El plano (@Primary) lo usa el cliente HTTP de Eureka (que en Spring Boot 4
 * usa RestClient); si el único builder fuera @LoadBalanced, Eureka trataría "localhost" como un
 * service-id y no podría registrarse. El balanceado (cualificado) es para llamadas inter-servicio.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean("hermesLoadBalancedRestClientBuilder")
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(
            @Value("${hermes.http.connect-timeout:2s}") Duration connectTimeout,
            @Value("${hermes.http.read-timeout:3s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(factory);
    }
}
