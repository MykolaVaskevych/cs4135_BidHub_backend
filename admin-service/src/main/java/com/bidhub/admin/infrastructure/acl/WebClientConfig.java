package com.bidhub.admin.infrastructure.acl;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /** Load-balanced WebClient resolving "account-service" via Eureka. */
    @Bean
    @LoadBalanced
    public WebClient accountAdminWebClient() {
        return WebClient.builder()
                .baseUrl("http://account-service")
                .build();
    }

    /** Load-balanced WebClient resolving "catalogue-service" via Eureka. */
    @Bean
    @LoadBalanced
    public WebClient catalogueWebClient() {
        return WebClient.builder()
                .baseUrl("http://catalog-service")
                .build();
    }

    /** Load-balanced WebClient resolving "auction-service" via Eureka. */
    @Bean
    @LoadBalanced
    public WebClient auctionWebClient() {
        return WebClient.builder()
                .baseUrl("http://auction-service")
                .build();
    }
}
