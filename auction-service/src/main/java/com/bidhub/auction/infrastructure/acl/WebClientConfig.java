package com.bidhub.auction.infrastructure.acl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * WebClient for account-service used by AccountClientBidValidationService and AccountClient.
     * The base URL includes the explicit port; Docker DNS resolves the hostname to the
     * account-service container, so Eureka-based load balancing is not required here. The
     * {@code X-Internal-Token} header is wired as a default header on the bean once, so every
     * caller of this WebClient is authenticated against account-service automatically and new
     * callers inherit it without having to remember the auth wiring.
     */
    @Bean
    public WebClient accountWebClient(
            @Value("${account.service.url:http://account-service:8081}") String accountUrl,
            @Value("${INTERNAL_API_TOKEN:}") String internalToken) {
        return WebClient.builder()
                .baseUrl(accountUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    @Bean
    public WebClient deliveryWebClient(
            @Value("${delivery.service.url:http://delivery-service:8088}") String deliveryUrl) {
        return WebClient.builder()
                .baseUrl(deliveryUrl)
                .build();
    }

    @Bean
    public WebClient notificationWebClient(
            @Value("${notification.service.url:http://notification-service:8086}") String notificationUrl) {
        return WebClient.builder()
                .baseUrl(notificationUrl)
                .build();
    }

    @Bean
    public WebClient catalogueWebClient(
            @Value("${catalogue.service.url:http://catalog-service:8082}") String catalogueUrl) {
        return WebClient.builder()
                .baseUrl(catalogueUrl)
                .build();
    }

    @Bean
    public WebClient paymentWebClient(
            @Value("${payment.service.url:http://payment-service:8085}") String paymentUrl) {
        return WebClient.builder()
                .baseUrl(paymentUrl)
                .build();
    }

    @Bean
    public WebClient orderWebClient(
            @Value("${order.service.url:http://order-service:8084}") String orderUrl) {
        return WebClient.builder()
                .baseUrl(orderUrl)
                .build();
    }
}
