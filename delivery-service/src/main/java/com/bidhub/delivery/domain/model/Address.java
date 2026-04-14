package com.bidhub.delivery.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    private String street;
    private String city;
    private String county;
    private String eircode;

    protected Address() {}

    private Address(String street, String city, String county, String eircode) {
        this.street = street;
        this.city = city;
        this.county = county;
        this.eircode = eircode;
    }

    /**
     * Factory method. INV-D1 adjacent: all address fields must be non-blank.
     */
    public static Address of(String street, String city, String county, String eircode) {
        if (street == null || street.isBlank()) throw new IllegalArgumentException("street must not be blank");
        if (city == null || city.isBlank()) throw new IllegalArgumentException("city must not be blank");
        if (county == null || county.isBlank()) throw new IllegalArgumentException("county must not be blank");
        if (eircode == null || eircode.isBlank()) throw new IllegalArgumentException("eircode must not be blank");
        return new Address(street, city, county, eircode);
    }

    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getCounty() { return county; }
    public String getEircode() { return eircode; }
}
