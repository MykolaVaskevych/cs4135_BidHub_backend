package com.bidhub.order.repository;

import com.bidhub.order.model.Order;
import com.bidhub.order.model.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    List<Order> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    List<Order> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(UUID buyerId, UUID sellerId);

    Optional<Order> findByAuctionId(UUID auctionId);

    List<Order> findByStatus(OrderStatus status);
}
