package com.bidhub.delivery.domain.repository;

import com.bidhub.delivery.domain.model.DeliveryJob;
import com.bidhub.delivery.domain.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryJobRepository extends JpaRepository<DeliveryJob, UUID> {

    Optional<DeliveryJob> findByOrderId(UUID orderId);

    List<DeliveryJob> findBySellerId(UUID sellerId);

    List<DeliveryJob> findByBuyerId(UUID buyerId);

    List<DeliveryJob> findByDriverId(UUID driverId);

    List<DeliveryJob> findByStatus(DeliveryStatus status);
}
