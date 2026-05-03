package com.bidhub.auction.infrastructure.acl;

public record AddressInfo(
        String addressLine1,
        String city,
        String county,
        String eircode) {}
