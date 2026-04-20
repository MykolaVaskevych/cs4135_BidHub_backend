package com.bidhub.admin.infrastructure.acl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient accountAdminWebClient(
            @Value("${account.service.url:http://account-service:8081}") String accountUrl) {
        return WebClient.builder().baseUrl(accountUrl).build();
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
