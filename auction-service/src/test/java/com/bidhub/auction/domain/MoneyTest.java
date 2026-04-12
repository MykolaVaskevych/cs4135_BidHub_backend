package com.bidhub.auction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.auction.domain.model.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    @DisplayName("Money.of uses EUR as default currency")
    void defaultCurrencyIsEur() {
        Money m = Money.of(BigDecimal.valueOf(10));
        assertThat(m.getCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Money.of with explicit currency stores it correctly")
    void explicitCurrencyIsStored() {
        Money m = Money.of(BigDecimal.valueOf(10), "USD");
        assertThat(m.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("isGreaterThan returns true when this amount is larger")
    void isGreaterThan_larger() {
        Money ten = Money.of(BigDecimal.valueOf(10));
        Money five = Money.of(BigDecimal.valueOf(5));
        assertThat(ten.isGreaterThan(five)).isTrue();
    }

    @Test
    @DisplayName("isGreaterThan returns false when amounts are equal")
    void isGreaterThan_equal() {
        Money a = Money.of(BigDecimal.valueOf(10));
        Money b = Money.of(BigDecimal.valueOf(10));
        assertThat(a.isGreaterThan(b)).isFalse();
    }

    @Test
    @DisplayName("isGreaterThan returns false when this amount is smaller")
    void isGreaterThan_smaller() {
        Money five = Money.of(BigDecimal.valueOf(5));
        Money ten = Money.of(BigDecimal.valueOf(10));
        assertThat(five.isGreaterThan(ten)).isFalse();
    }

    @Test
    @DisplayName("add returns correct sum")
    void add_returnsSum() {
        Money a = Money.of(BigDecimal.valueOf(10));
        Money b = Money.of(BigDecimal.valueOf(5));
        Money result = a.add(b);
        assertThat(result.getAmount().compareTo(BigDecimal.valueOf(15))).isZero();
        assertThat(result.getCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("subtract returns correct difference")
    void subtract_returnsDifference() {
        Money a = Money.of(BigDecimal.valueOf(10));
        Money b = Money.of(BigDecimal.valueOf(3));
        Money result = a.subtract(b);
        assertThat(result.getAmount().compareTo(BigDecimal.valueOf(7))).isZero();
    }

    @Test
    @DisplayName("add throws when currencies differ")
    void add_differentCurrencies_throws() {
        Money eur = Money.of(BigDecimal.valueOf(10), "EUR");
        Money usd = Money.of(BigDecimal.valueOf(5), "USD");
        assertThatThrownBy(() -> eur.add(usd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Money instances with equal amount and currency are equal")
    void equality_sameAmountAndCurrency() {
        Money a = Money.of(BigDecimal.valueOf(10));
        Money b = Money.of(BigDecimal.valueOf(10));
        assertThat(a).isEqualTo(b);
    }
}
