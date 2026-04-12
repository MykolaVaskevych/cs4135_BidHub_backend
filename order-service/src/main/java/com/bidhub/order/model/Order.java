package com.bidhub.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(nullable = false)
    private UUID auctionId;

    @Column(nullable = false)
    private UUID buyerId;

    @Column(nullable = false)
    private UUID sellerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.AWAITING_COLLECTION;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmCollection() {
        if (this.status != OrderStatus.AWAITING_COLLECTION) {
            throw new IllegalStateException(
                    "Can only confirm collection from AWAITING_COLLECTION state");
        }
        this.status = OrderStatus.BEING_DELIVERED;
    }

    public void markDelivered() {
        if (this.status != OrderStatus.BEING_DELIVERED) {
            throw new IllegalStateException("Can only mark delivered from BEING_DELIVERED state");
        }
        this.status = OrderStatus.DELIVERED;
    }

    public void confirmDelivery() {
        if (this.status != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Can only confirm delivery from DELIVERED state");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void markDisputed() {
        this.status = OrderStatus.DISPUTED;
    }
}
