package com.bidhub.delivery.application.service;

import com.bidhub.delivery.application.dto.CreateDeliveryJobRequest;
import com.bidhub.delivery.application.dto.DeliveryJobResponse;
import com.bidhub.delivery.domain.exception.DeliveryJobNotFoundException;
import com.bidhub.delivery.domain.model.DeliveryJob;
import com.bidhub.delivery.domain.model.EscrowReference;
import com.bidhub.delivery.domain.repository.DeliveryJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeliveryEscrowService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEscrowService.class);

    private final DeliveryJobRepository repo;

    public DeliveryEscrowService(DeliveryJobRepository repo) {
        this.repo = repo;
    }

    /**
     * INV-D1: Creates a delivery job. orderId, sellerId, buyerId, addresses, escrowRef required.
     */
    public DeliveryJobResponse createJob(CreateDeliveryJobRequest req) {
        EscrowReference escrow = EscrowReference.of(req.escrowId(), req.escrowAmount(), "EUR");
        DeliveryJob job = DeliveryJob.create(
                req.orderId(), req.auctionId(),
                req.sellerId(), req.buyerId(),
                req.pickupAddress().toDomain(),
                req.deliveryAddress().toDomain(),
                escrow);
        repo.save(job);
        log.info("[DELIVERY] created job={} order={}", job.getDeliveryJobId(), job.getOrderId());
        return DeliveryJobResponse.from(job);
    }

    /** INV-D2: Assigns a driver. Job must be in PENDING state. */
    public DeliveryJobResponse assignDriver(UUID jobId, UUID driverId) {
        DeliveryJob job = getJobEntity(jobId);
        job.assignDriver(driverId);
        repo.save(job);
        return DeliveryJobResponse.from(job);
    }

    /** INV-D3: Seller confirms collection. Job must be ASSIGNED; sellerId must match. */
    public DeliveryJobResponse confirmCollection(UUID jobId, UUID sellerId) {
        DeliveryJob job = getJobEntity(jobId);
        job.confirmCollection(sellerId);
        repo.save(job);
        return DeliveryJobResponse.from(job);
    }

    /** Driver marks goods as delivered. Job must be IN_TRANSIT; driverId must match. */
    public DeliveryJobResponse markDelivered(UUID jobId, UUID driverId) {
        DeliveryJob job = getJobEntity(jobId);
        job.markDelivered(driverId);
        repo.save(job);
        return DeliveryJobResponse.from(job);
    }

    /** INV-D4: Buyer confirms delivery. Job must be DELIVERED; buyerId must match. Escrow released. */
    public DeliveryJobResponse confirmDelivery(UUID jobId, UUID buyerId) {
        DeliveryJob job = getJobEntity(jobId);
        job.confirmDelivery(buyerId);
        repo.save(job);
        log.info("[DELIVERY] confirmed delivery job={} — publishing DeliveryConfirmed event (async, consumed by Payment context)", jobId);
        return DeliveryJobResponse.from(job);
    }

    /** INV-D5/D6: Buyer raises a dispute. Job must be DELIVERED. Escrow frozen. */
    public DeliveryJobResponse raiseDispute(UUID jobId, UUID reporterId, String reason) {
        DeliveryJob job = getJobEntity(jobId);
        job.raiseDispute(reporterId, reason);
        repo.save(job);
        log.info("[DELIVERY] dispute raised job={} reporter={} reason={}", jobId, reporterId, reason);
        return DeliveryJobResponse.from(job);
    }

    /** Cancel job. Only valid from PENDING or ASSIGNED. */
    public DeliveryJobResponse cancelJob(UUID jobId) {
        DeliveryJob job = getJobEntity(jobId);
        job.cancel();
        repo.save(job);
        return DeliveryJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public DeliveryJobResponse getJob(UUID jobId) {
        return DeliveryJobResponse.from(getJobEntity(jobId));
    }

    @Transactional(readOnly = true)
    public List<DeliveryJobResponse> getMyJobs(UUID userId) {
        List<DeliveryJob> asBuyer = repo.findByBuyerId(userId);
        List<DeliveryJob> asSeller = repo.findBySellerId(userId);
        List<DeliveryJob> asDriver = repo.findByDriverId(userId);
        return java.util.stream.Stream.of(asBuyer, asSeller, asDriver)
                .flatMap(List::stream)
                .distinct()
                .map(DeliveryJobResponse::from)
                .toList();
    }

    private DeliveryJob getJobEntity(UUID jobId) {
        return repo.findById(jobId)
                .orElseThrow(() -> new DeliveryJobNotFoundException(jobId));
    }
}
