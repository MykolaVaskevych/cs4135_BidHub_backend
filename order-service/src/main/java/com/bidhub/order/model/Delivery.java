package com.bidhub.order.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deliveryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    private UUID driverId;

    private LocalDateTime acceptedAt;

    private LocalDateTime collectedAt;

    private LocalDateTime deliveredAt;

    public void acceptJob(UUID driverId) {
        this.driverId = driverId;
        this.acceptedAt = LocalDateTime.now();
    }

    public void markCollected() {
        this.collectedAt = LocalDateTime.now();
        this.order.confirmCollection();
    }

    public void markDelivered() {
        this.deliveredAt = LocalDateTime.now();
        this.order.markDelivered();
    }
}
