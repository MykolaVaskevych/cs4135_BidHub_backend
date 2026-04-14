package com.bidhub.delivery.domain;

import com.bidhub.delivery.domain.model.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AddressTest {

    @Test
    @DisplayName("Address.of() creates valid address")
    void of_validFields_returnsAddress() {
        Address a = Address.of("1 Main St", "Dublin", "Dublin", "D01 AA01");
        assertThat(a.getStreet()).isEqualTo("1 Main St");
        assertThat(a.getCity()).isEqualTo("Dublin");
        assertThat(a.getCounty()).isEqualTo("Dublin");
        assertThat(a.getEircode()).isEqualTo("D01 AA01");
    }

    @Test
    @DisplayName("Address.of() throws on blank street")
    void of_blankStreet_throws() {
        assertThatThrownBy(() -> Address.of("", "Dublin", "Dublin", "D01 AA01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Address.of() throws on null city")
    void of_nullCity_throws() {
        assertThatThrownBy(() -> Address.of("1 Main St", null, "Dublin", "D01 AA01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Address.of() throws on blank eircode")
    void of_blankEircode_throws() {
        assertThatThrownBy(() -> Address.of("1 Main St", "Dublin", "Dublin", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
