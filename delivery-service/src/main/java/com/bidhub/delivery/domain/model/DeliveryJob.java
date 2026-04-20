package com.bidhub.delivery.domain.model;

import com.bidhub.delivery.domain.exception.IllegalDeliveryStateException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_jobs")
public class DeliveryJob {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID deliveryJobId;

    @Column(nullable = false, updatable = false)
    private UUID orderId;

    private UUID auctionId;

    @Column(nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private UUID buyerId;

    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street",  column = @Column(name = "pickup_street")),
            @AttributeOverride(name = "city",    column = @Column(name = "pickup_city")),
            @AttributeOverride(name = "county",  column = @Column(name = "pickup_county")),
            @AttributeOverride(name = "eircode", column = @Column(name = "pickup_eircode"))
    })
    private Address pickupAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street",  column = @Column(name = "delivery_street")),
            @AttributeOverride(name = "city",    column = @Column(name = "delivery_city")),
            @AttributeOverride(name = "county",  column = @Column(name = "delivery_county")),
            @AttributeOverride(name = "eircode", column = @Column(name = "delivery_eircode"))
    })
    private Address deliveryAddress;

    @Embedded
    private EscrowReference escrowRef;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected DeliveryJob() {}

    /**
     * Factory. INV-D1: orderId, sellerId, buyerId, addresses, and escrowRef must be non-null.
     */
    public static DeliveryJob create(UUID orderId, UUID auctionId,
                                     UUID sellerId, UUID buyerId,
                                     Address pickupAddress, Address deliveryAddress,
                                     EscrowReference escrowRef) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        if (sellerId == null) throw new IllegalArgumentException("sellerId must not be null");
        if (buyerId == null) throw new IllegalArgumentException("buyerId must not be null");
        if (pickupAddress == null) throw new IllegalArgumentException("pickupAddress must not be null");
        if (deliveryAddress == null) throw new IllegalArgumentException("deliveryAddress must not be null");
        if (escrowRef == null) throw new IllegalArgumentException("escrowRef must not be null");

        DeliveryJob job = new DeliveryJob();
        job.deliveryJobId = UUID.randomUUID();
        job.orderId = orderId;
        job.auctionId = auctionId;
        job.sellerId = sellerId;
        job.buyerId = buyerId;
        job.pickupAddress = pickupAddress;
        job.deliveryAddress = deliveryAddress;
        job.escrowRef = escrowRef;
        job.status = DeliveryStatus.PENDING;
        job.createdAt = Instant.now();
        job.updatedAt = job.createdAt;
        return job;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * INV-D2: Driver can only be assigned when status is PENDING.
     */
    public void assignDriver(UUID driverId) {
        if (this.status != DeliveryStatus.PENDING) {
            throw new IllegalDeliveryStateException(
                    "assignDriver", this.status, DeliveryStatus.PENDING);
        }
        this.driverId = driverId;
        this.status = DeliveryStatus.ASSIGNED;
    }

    /**
     * INV-D3: Seller confirms collection, transitions ASSIGNED → IN_TRANSIT.
     */
    public void confirmCollection(UUID sellerId) {
        if (this.status != DeliveryStatus.ASSIGNED) {
            throw new IllegalDeliveryStateException(
                    "confirmCollection", this.status, DeliveryStatus.ASSIGNED);
        }
        if (!this.sellerId.equals(sellerId)) {
            throw new IllegalDeliveryStateException("confirmCollection: sellerId mismatch");
        }
        this.status = DeliveryStatus.IN_TRANSIT;
    }

    /**
     * Driver marks goods delivered. IN_TRANSIT → DELIVERED.
     */
    public void markDelivered(UUID driverId) {
        if (this.status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalDeliveryStateException(
                    "markDelivered", this.status, DeliveryStatus.IN_TRANSIT);
        }
        if (!this.driverId.equals(driverId)) {
            throw new IllegalDeliveryStateException("markDelivered: driverId mismatch");
        }
        this.status = DeliveryStatus.DELIVERED;
    }

    /**
     * INV-D4: Buyer confirms delivery. DELIVERED → CONFIRMED; escrow is released.
     */
    public void confirmDelivery(UUID buyerId) {
        if (this.status != DeliveryStatus.DELIVERED) {
            throw new IllegalDeliveryStateException(
                    "confirmDelivery", this.status, DeliveryStatus.DELIVERED);
        }
        if (!this.buyerId.equals(buyerId)) {
            throw new IllegalDeliveryStateException("confirmDelivery: buyerId mismatch");
        }
        this.status = DeliveryStatus.CONFIRMED;
        this.escrowRef.release();
    }

    /**
     * INV-D5/D6: Buyer raises dispute. DELIVERED → DISPUTED; escrow frozen.
     */
    public void raiseDispute(UUID reporterId, String reason) {
        if (this.status != DeliveryStatus.DELIVERED) {
            throw new IllegalDeliveryStateException(
                    "raiseDispute", this.status, DeliveryStatus.DELIVERED);
        }
        this.status = DeliveryStatus.DISPUTED;
        this.escrowRef.dispute();
    }

    /**
     * Admin resolves a dispute. DISPUTED → CONFIRMED; escrow released.
     */
    public void resolveDispute() {
        if (this.status != DeliveryStatus.DISPUTED) {
            throw new IllegalDeliveryStateException(
                    "resolveDispute", this.status, DeliveryStatus.DISPUTED);
        }
        this.status = DeliveryStatus.CONFIRMED;
        this.escrowRef.release();
    }

    /**
     * Cancel job. Only valid from PENDING or ASSIGNED.
     */
    public void cancel() {
        if (this.status != DeliveryStatus.PENDING && this.status != DeliveryStatus.ASSIGNED) {
            throw new IllegalDeliveryStateException(
                    "cancel: can only cancel PENDING or ASSIGNED job, current=" + this.status);
        }
        this.status = DeliveryStatus.CANCELLED;
    }

    public UUID getDeliveryJobId() { return deliveryJobId; }
    public UUID getOrderId() { return orderId; }
    public UUID getAuctionId() { return auctionId; }
    public UUID getSellerId() { return sellerId; }
    public UUID getBuyerId() { return buyerId; }
    public UUID getDriverId() { return driverId; }
    public DeliveryStatus getStatus() { return status; }
    public Address getPickupAddress() { return pickupAddress; }
    public Address getDeliveryAddress() { return deliveryAddress; }
    public EscrowReference getEscrowRef() { return escrowRef; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
