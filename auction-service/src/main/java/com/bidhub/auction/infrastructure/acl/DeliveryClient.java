package com.bidhub.auction.infrastructure.acl;

import com.bidhub.auction.domain.exception.BuyNowDownstreamException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DeliveryClient {

    private final WebClient deliveryWebClient;

    public DeliveryClient(WebClient deliveryWebClient) {
        this.deliveryWebClient = deliveryWebClient;
    }

    public UUID createDeliveryJob(
            UUID orderId,
            UUID auctionId,
            UUID sellerId,
            UUID buyerId,
            AddressInfo pickupAddress,
            AddressInfo deliveryAddress,
            UUID paymentTransactionId,
            BigDecimal amount) {
        try {
            Map<String, Object> body =
                    Map.of(
                            "orderId", orderId,
                            "auctionId", auctionId,
                            "sellerId", sellerId,
                            "buyerId", buyerId,
                            "pickupAddress", toRequestAddress(pickupAddress),
                            "deliveryAddress", toRequestAddress(deliveryAddress),
                            "escrowId", paymentTransactionId,
                            "escrowAmount", amount);
            DeliveryJobView response =
                    deliveryWebClient
                            .post()
                            .uri("/api/delivery")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(DeliveryJobView.class)
                            .block();
            if (response == null || response.deliveryJobId() == null) {
                throw new BuyNowDownstreamException(
                        "delivery-service returned no deliveryJobId for orderId=" + orderId);
            }
            return response.deliveryJobId();
        } catch (BuyNowDownstreamException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuyNowDownstreamException(
                    "delivery-service unavailable while creating delivery for orderId=" + orderId,
                    ex);
        }
    }

    private Map<String, String> toRequestAddress(AddressInfo address) {
        return Map.of(
                "street", address.addressLine1(),
                "city", address.city(),
                "county", address.county(),
                "eircode", address.eircode());
    }

    record DeliveryJobView(UUID deliveryJobId, UUID orderId) {}
}
