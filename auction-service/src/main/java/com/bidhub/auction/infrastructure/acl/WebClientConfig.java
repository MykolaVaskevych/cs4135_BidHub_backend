package com.bidhub.auction.infrastructure.acl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Load-balanced WebClient that resolves "account-service" via Eureka.
     * Used by AccountClientBidValidationService.
     */
    @Bean
    @LoadBalanced
    public WebClient accountWebClient() {
        return WebClient.builder()
                .baseUrl("http://account-service")
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
