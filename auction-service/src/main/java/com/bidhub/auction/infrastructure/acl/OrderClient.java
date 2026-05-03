package com.bidhub.auction.infrastructure.acl;

import com.bidhub.auction.domain.exception.BuyNowDownstreamException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OrderClient {

    private final WebClient orderWebClient;

    public OrderClient(WebClient orderWebClient) {
        this.orderWebClient = orderWebClient;
    }

    public UUID createOrder(UUID auctionId, UUID buyerId, UUID sellerId, BigDecimal amount) {
        try {
            OrderView response =
                    orderWebClient
                            .post()
                            .uri("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(
                                    Map.of(
                                            "auctionId", auctionId,
                                            "buyerId", buyerId,
                                            "sellerId", sellerId,
                                            "amount", amount))
                            .retrieve()
                            .bodyToMono(OrderView.class)
                            .block();
            if (response == null || response.orderId() == null) {
                throw new BuyNowDownstreamException(
                        "order-service returned no orderId for auctionId=" + auctionId);
            }
            return response.orderId();
        } catch (BuyNowDownstreamException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuyNowDownstreamException(
                    "order-service unavailable while creating order for auctionId=" + auctionId,
                    ex);
        }
    }

    public void cancelOrder(UUID orderId) {
        orderWebClient
                .patch()
                .uri("/api/orders/{orderId}/cancel", orderId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OrderView(UUID orderId, UUID auctionId, UUID buyerId, UUID sellerId, String status) {}
}
