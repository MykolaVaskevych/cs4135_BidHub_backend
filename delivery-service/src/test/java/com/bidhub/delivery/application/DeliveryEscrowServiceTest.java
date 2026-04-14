package com.bidhub.delivery.application;

import com.bidhub.delivery.application.dto.AddressDto;
import com.bidhub.delivery.application.dto.CreateDeliveryJobRequest;
import com.bidhub.delivery.application.dto.DeliveryJobResponse;
import com.bidhub.delivery.application.service.DeliveryEscrowService;
import com.bidhub.delivery.domain.exception.DeliveryJobNotFoundException;
import com.bidhub.delivery.domain.exception.IllegalDeliveryStateException;
import com.bidhub.delivery.domain.model.*;
import com.bidhub.delivery.domain.repository.DeliveryJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryEscrowServiceTest {

    @Mock DeliveryJobRepository repo;
    @InjectMocks DeliveryEscrowService service;

    private static final UUID ORDER_ID  = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID  = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    private CreateDeliveryJobRequest createReq() {
        AddressDto pickup   = new AddressDto("1 Main St", "Dublin", "Dublin", "D01 AA01");
        AddressDto delivery = new AddressDto("2 High St", "Cork",   "Cork",   "T12 BB02");
        return new CreateDeliveryJobRequest(ORDER_ID, null, SELLER_ID, BUYER_ID,
                pickup, delivery, UUID.randomUUID(), new BigDecimal("100.00"));
    }

    private DeliveryJob savedJob() {
        Address pickup   = Address.of("1 Main St", "Dublin", "Dublin", "D01 AA01");
        Address delivery = Address.of("2 High St", "Cork",   "Cork",   "T12 BB02");
        EscrowReference escrow = EscrowReference.of(UUID.randomUUID(), new BigDecimal("100.00"), "EUR");
        return DeliveryJob.create(ORDER_ID, null, SELLER_ID, BUYER_ID, pickup, delivery, escrow);
    }

    @Test
    @DisplayName("createJob() creates PENDING job")
    void createJob_returnsPendingResponse() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DeliveryJobResponse resp = service.createJob(createReq());
        assertThat(resp.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(resp.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("assignDriver() transitions to ASSIGNED")
    void assignDriver_returnsAssignedResponse() {
        DeliveryJob job = savedJob();
        when(repo.findById(job.getDeliveryJobId())).thenReturn(Optional.of(job));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DeliveryJobResponse resp = service.assignDriver(job.getDeliveryJobId(), DRIVER_ID);
        assertThat(resp.status()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(resp.driverId()).isEqualTo(DRIVER_ID);
    }

    @Test
    @DisplayName("Full happy path: create→assign→collect→deliver→confirm")
    void fullHappyPath_escrowReleased() {
        DeliveryJob job = savedJob();
        UUID jobId = job.getDeliveryJobId();
        when(repo.findById(jobId)).thenReturn(Optional.of(job));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignDriver(jobId, DRIVER_ID);
        service.confirmCollection(jobId, SELLER_ID);
        service.markDelivered(jobId, DRIVER_ID);
        DeliveryJobResponse resp = service.confirmDelivery(jobId, BUYER_ID);

        assertThat(resp.status()).isEqualTo(DeliveryStatus.CONFIRMED);
        assertThat(job.getEscrowRef().getEscrowStatus()).isEqualTo(EscrowStatus.RELEASED);
    }

    @Test
    @DisplayName("raiseDispute() freezes escrow")
    void raiseDispute_escrowFrozen() {
        DeliveryJob job = savedJob();
        UUID jobId = job.getDeliveryJobId();
        when(repo.findById(jobId)).thenReturn(Optional.of(job));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignDriver(jobId, DRIVER_ID);
        service.confirmCollection(jobId, SELLER_ID);
        service.markDelivered(jobId, DRIVER_ID);
        DeliveryJobResponse resp = service.raiseDispute(jobId, BUYER_ID, "Goods damaged");

        assertThat(resp.status()).isEqualTo(DeliveryStatus.DISPUTED);
        assertThat(job.getEscrowRef().getEscrowStatus()).isEqualTo(EscrowStatus.DISPUTED);
    }

    @Test
    @DisplayName("cancelJob() cancels PENDING job")
    void cancelJob_cancels() {
        DeliveryJob job = savedJob();
        UUID jobId = job.getDeliveryJobId();
        when(repo.findById(jobId)).thenReturn(Optional.of(job));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeliveryJobResponse resp = service.cancelJob(jobId);
        assertThat(resp.status()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("getJob() throws when not found")
    void getJob_notFound_throws() {
        UUID missing = UUID.randomUUID();
        when(repo.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getJob(missing))
                .isInstanceOf(DeliveryJobNotFoundException.class);
    }
}
