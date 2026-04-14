package com.bidhub.delivery.domain;

import com.bidhub.delivery.domain.exception.IllegalDeliveryStateException;
import com.bidhub.delivery.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DeliveryJobInvariantTest {

    private static final UUID ORDER_ID  = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID  = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    private DeliveryJob newJob() {
        Address pickup   = Address.of("1 Main St", "Dublin", "Dublin", "D01 AA01");
        Address delivery = Address.of("2 High St", "Cork",   "Cork",   "T12 BB02");
        EscrowReference escrow = EscrowReference.of(UUID.randomUUID(), new BigDecimal("100.00"), "EUR");
        return DeliveryJob.create(ORDER_ID, null, SELLER_ID, BUYER_ID, pickup, delivery, escrow);
    }

    @Test
    @DisplayName("INV-D1: null orderId throws")
    void create_nullOrderId_throws() {
        Address a = Address.of("1 Main St", "Dublin", "Dublin", "D01 AA01");
        EscrowReference e = EscrowReference.of(UUID.randomUUID(), BigDecimal.TEN, "EUR");
        assertThatThrownBy(() -> DeliveryJob.create(null, null, SELLER_ID, BUYER_ID, a, a, e))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("INV-D1: initial status is PENDING")
    void create_initialStatusPending() {
        assertThat(newJob().getStatus()).isEqualTo(DeliveryStatus.PENDING);
    }

    @Test
    @DisplayName("INV-D2: assignDriver transitions PENDING → ASSIGNED")
    void assignDriver_fromPending_becomesAssigned() {
        DeliveryJob job = newJob();
        job.assignDriver(DRIVER_ID);
        assertThat(job.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(job.getDriverId()).isEqualTo(DRIVER_ID);
    }

    @Test
    @DisplayName("INV-D2: assignDriver throws when not PENDING")
    void assignDriver_notPending_throws() {
        DeliveryJob job = newJob();
        job.assignDriver(DRIVER_ID);
        assertThatThrownBy(() -> job.assignDriver(DRIVER_ID))
                .isInstanceOf(IllegalDeliveryStateException.class);
    }

    @Test
    @DisplayName("INV-D3: confirmCollection transitions ASSIGNED → IN_TRANSIT")
    void confirmCollection_fromAssigned_becomesInTransit() {
        DeliveryJob job = newJob();
        job.assignDriver(DRIVER_ID);
        job.confirmCollection(SELLER_ID);
        assertThat(job.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
    }

    @Test
    @DisplayName("INV-D3: confirmCollection rejects wrong sellerId")
    void confirmCollection_wrongSeller_throws() {
        DeliveryJob job = newJob();
        job.assignDriver(DRIVER_ID);
        assertThatThrownBy(() -> job.confirmCollection(UUID.randomUUID()))
                .isInstanceOf(IllegalDeliveryStateException.class);
    }

    @Test
    @DisplayName("INV-D4: confirmDelivery releases escrow")
    void confirmDelivery_releasesEscrow() {
        DeliveryJob job = newJob();
        job.assignDriver(DRIVER_ID);
        job.confirmCollection(SELLER_ID);
        job.markDelivered(DRIVER_ID);
        job.confirmDelivery(BUYER_ID);
        assertThat(job.getStatus()).isEqualTo(DeliveryStatus.CONFIRMED);
        assertThat(job.getEscrowRef().getEscrowStatus()).isEqualTo(EscrowStatus.RELEASED);
    }

    @Test
    @DisplayName("INV-D5/D6: raiseDispute freezes escrow")
    void raiseDispute_freezesEscrow() {
        DeliveryJob job = newJob();
        job.assignDriver(DRIVER_ID);
        job.confirmCollection(SELLER_ID);
        job.markDelivered(DRIVER_ID);
        job.raiseDispute(BUYER_ID, "Goods damaged");
        assertThat(job.getStatus()).isEqualTo(DeliveryStatus.DISPUTED);
        assertThat(job.getEscrowRef().getEscrowStatus()).isEqualTo(EscrowStatus.DISPUTED);
    }

    @Test
    @DisplayName("cancel: PENDING job can be cancelled")
    void cancel_fromPending_becomesCancelled() {
        DeliveryJob job = newJob();
        job.cancel();
        assertThat(job.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel: throws when job is IN_TRANSIT")
    void cancel_fromInTransit_throws() {
        DeliveryJob job = newJob();
        job.assignDriver(DRIVER_ID);
        job.confirmCollection(SELLER_ID);
        assertThatThrownBy(job::cancel).isInstanceOf(IllegalDeliveryStateException.class);
    }
}
