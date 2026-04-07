package com.bidhub.account.dto;

import com.bidhub.account.model.ShippingAddress;
import java.util.UUID;

public record AddressResponse(
        UUID addressId,
        String addressLine1,
        String addressLine2,
        String city,
        String county,
        String eircode,
        boolean isDefault) {

    public static AddressResponse fromEntity(ShippingAddress address) {
        return new AddressResponse(
                address.getAddressId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getCounty(),
                address.getEircode(),
                address.getIsDefault());
    }
}
