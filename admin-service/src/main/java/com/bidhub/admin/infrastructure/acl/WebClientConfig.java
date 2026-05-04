package com.bidhub.admin.infrastructure.acl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * WebClient for account-service admin endpoints. The {@code X-Internal-Token} header is
     * wired as a default header so every caller of this WebClient is authenticated against
     * account-service automatically; new callers inherit it without having to remember the auth
     * wiring.
     */
    @Bean
    public WebClient accountAdminWebClient(
            @Value("${account.service.url:http://account-service:8081}") String accountUrl,
            @Value("${INTERNAL_API_TOKEN:}") String internalToken) {
        return WebClient.builder()
                .baseUrl(accountUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    @Bean
    public WebClient catalogueWebClient(
            @Value("${catalogue.service.url:http://catalog-service:8082}") String catalogueUrl) {
        return WebClient.builder().baseUrl(catalogueUrl).build();
    }

    @Bean
    public WebClient auctionWebClient(
            @Value("${auction.service.url:http://auction-service:8083}") String auctionUrl) {
        return WebClient.builder().baseUrl(auctionUrl).build();
    }
}
