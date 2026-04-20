package com.bidhub.auction.infrastructure.acl;

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
    @LoadBalanced
    public WebClient deliveryWebClient() {
        return WebClient.builder()
                .baseUrl("http://delivery-service")
                .build();
    }
}
