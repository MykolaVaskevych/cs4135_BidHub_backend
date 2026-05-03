package com.bidhub.auction.infrastructure.acl;

import com.bidhub.auction.domain.exception.BuyNowDownstreamException;
import com.bidhub.auction.domain.exception.InsufficientWalletFundsException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PaymentClient {

    private final WebClient paymentWebClient;

    public PaymentClient(WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
    }

    public ChargeResult charge(UUID buyerId, BigDecimal amount, String description) {
        try {
            ChargeView response =
                    paymentWebClient
                            .post()
                            .uri("/api/payments/wallet/charge")
                            .header("X-User-Id", buyerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("amount", amount, "description", description))
                            .retrieve()
                            .onStatus(HttpStatusCode::is4xxClientError, this::map4xx)
                            .bodyToMono(ChargeView.class)
                            .block();
            if (response == null || response.transactionId() == null) {
                throw new BuyNowDownstreamException(
                        "payment-service returned no transactionId for buyerId=" + buyerId);
            }
            return new ChargeResult(response.transactionId(), response.balance());
        } catch (BuyNowDownstreamException | InsufficientWalletFundsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuyNowDownstreamException(
                    "payment-service unavailable while charging buyerId=" + buyerId, ex);
        }
    }

    public void refund(UUID buyerId, BigDecimal amount) {
        paymentWebClient
                .post()
                .uri("/api/payments/wallet/top-up")
                .header("X-User-Id", buyerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private Mono<? extends Throwable> map4xx(
            org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(PaymentErrorView.class)
                .defaultIfEmpty(new PaymentErrorView("UNKNOWN", null))
                .map(
                        err -> {
                            String code = err.error() != null ? err.error() : "UNKNOWN";
                            String message =
                                    err.message() != null
                                            ? err.message()
                                            : "payment-service " + code;
                            if ("INSUFFICIENT_FUNDS".equals(code)
                                    || "WALLET_NOT_FOUND".equals(code)) {
                                return (Throwable) new InsufficientWalletFundsException(message);
                            }
                            return (Throwable)
                                    new BuyNowDownstreamException(
                                            "payment-service rejected charge ["
                                                    + code
                                                    + "]: "
                                                    + message);
                        });
    }

    public record ChargeResult(UUID transactionId, BigDecimal newBalance) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChargeView(
            UUID transactionId,
            UUID walletId,
            UUID userId,
            BigDecimal balance,
            String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentErrorView(String error, String message) {}
}
