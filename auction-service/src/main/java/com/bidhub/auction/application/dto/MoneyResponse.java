package com.bidhub.auction.application.dto;

import com.bidhub.auction.domain.model.Money;
import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, String currency) {

    public static MoneyResponse from(Money money) {
        if (money == null) return null;
        return new MoneyResponse(money.getAmount(), money.getCurrency());
    }
}
