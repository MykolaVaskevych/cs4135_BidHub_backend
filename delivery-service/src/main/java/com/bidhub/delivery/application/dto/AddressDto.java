package com.bidhub.delivery.application.dto;

import com.bidhub.delivery.domain.model.Address;
import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String county,
        @NotBlank String eircode
) {
    public Address toDomain() {
        return Address.of(street, city, county, eircode);
    }

    public static AddressDto from(Address a) {
        return new AddressDto(a.getStreet(), a.getCity(), a.getCounty(), a.getEircode());
    }
}
