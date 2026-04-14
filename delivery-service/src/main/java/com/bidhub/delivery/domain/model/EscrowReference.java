package com.bidhub.delivery.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class EscrowReference {

    private UUID escrowId;
    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private EscrowStatus escrowStatus;

    protected EscrowReference() {}

    private EscrowReference(UUID escrowId, BigDecimal amount, String currency) {
        this.escrowId = escrowId;
        this.amount = amount;
        this.currency = currency;
        this.escrowStatus = EscrowStatus.HELD;
    }

    public static EscrowReference of(UUID escrowId, BigDecimal amount, String currency) {
        if (escrowId == null) throw new IllegalArgumentException("escrowId must not be null");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("escrow amount must be positive");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency must not be blank");
        return new EscrowReference(escrowId, amount, currency);
    }

    /** INV-D4: Release escrow on confirmed delivery. */
    public void release() {
        this.escrowStatus = EscrowStatus.RELEASED;
    }

    /** INV-D5/D6: Move escrow to disputed state. */
    public void dispute() {
        this.escrowStatus = EscrowStatus.DISPUTED;
    }

    public UUID getEscrowId() { return escrowId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public EscrowStatus getEscrowStatus() { return escrowStatus; }
}
