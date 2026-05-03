package com.bidhub.auction.infrastructure.acl;

import com.bidhub.auction.domain.exception.BuyNowDownstreamException;
import com.bidhub.auction.domain.exception.MissingShippingAddressException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AccountClient {

    private final WebClient accountWebClient;

    public AccountClient(WebClient accountWebClient) {
        this.accountWebClient = accountWebClient;
    }

    public AddressInfo defaultAddressOf(UUID userId, String role) {
        AccountAddressView view = fetch(userId);
        if (view.addresses() == null || view.addresses().isEmpty()) {
            throw new MissingShippingAddressException(userId, role);
        }
        return view.addresses().stream()
                .filter(a -> Boolean.TRUE.equals(a.isDefault()))
                .findFirst()
                .map(this::toAddressInfo)
                .orElseThrow(() -> new MissingShippingAddressException(userId, role));
    }

    private AccountAddressView fetch(UUID userId) {
        try {
            AccountAddressView response =
                    accountWebClient
                            .get()
                            .uri("/api/accounts/{userId}", userId)
                            .retrieve()
                            .bodyToMono(AccountAddressView.class)
                            .block();
            if (response == null) {
                throw new BuyNowDownstreamException(
                        "account-service returned no body for userId=" + userId);
            }
            return response;
        } catch (BuyNowDownstreamException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuyNowDownstreamException(
                    "account-service unavailable for userId=" + userId, ex);
        }
    }

    private AddressInfo toAddressInfo(AddressView address) {
        return new AddressInfo(
                address.addressLine1(),
                address.city(),
                address.county(),
                address.eircode());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountAddressView(UUID userId, List<AddressView> addresses) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AddressView(
            UUID addressId,
            String addressLine1,
            String city,
            String county,
            String eircode,
            Boolean isDefault) {}
}
