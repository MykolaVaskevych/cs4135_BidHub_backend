package com.bidhub.order.service;

import com.bidhub.order.dto.*;
import com.bidhub.order.exception.DeliveryNotFoundException;
import com.bidhub.order.exception.InvalidOrderStateException;
import com.bidhub.order.exception.OrderNotFoundException;
import com.bidhub.order.model.Delivery;
import com.bidhub.order.model.Order;
import com.bidhub.order.model.OrderStatus;
import com.bidhub.order.repository.DeliveryRepository;
import com.bidhub.order.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order =
                Order.builder()
                        .auctionId(request.auctionId())
                        .buyerId(request.buyerId())
                        .sellerId(request.sellerId())
                        .amount(request.amount())
                        .build();
        order = orderRepository.save(order);

        deliveryRepository.save(Delivery.builder().order(order).build());

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        return toResponse(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCompletedOrders(UUID userId) {
        return orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId).stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryByOrderId(UUID orderId) {
        Delivery delivery =
                deliveryRepository
                        .findByOrderOrderId(orderId)
                        .orElseThrow(
                                () ->
                                        new DeliveryNotFoundException(
                                                "Delivery not found for order: " + orderId));
        return toDeliveryResponse(delivery);
    }

    @Transactional
    public OrderResponse confirmCollection(UUID orderId) {
        Order order = findOrder(orderId);
        try {
            order.confirmCollection();
        } catch (IllegalStateException e) {
            throw new InvalidOrderStateException(e.getMessage());
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse markDelivered(UUID orderId) {
        Order order = findOrder(orderId);
        try {
            order.markDelivered();
        } catch (IllegalStateException e) {
            throw new InvalidOrderStateException(e.getMessage());
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirmDelivery(UUID orderId) {
        Order order = findOrder(orderId);
        try {
            order.confirmDelivery();
        } catch (IllegalStateException e) {
            throw new InvalidOrderStateException(e.getMessage());
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = findOrder(orderId);
        try {
            order.cancel();
        } catch (IllegalStateException e) {
            throw new InvalidOrderStateException(e.getMessage());
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    private Order findOrder(UUID orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getAuctionId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getAmount(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private DeliveryResponse toDeliveryResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getDeliveryId(),
                delivery.getOrder().getOrderId(),
                delivery.getDriverId(),
                delivery.getAcceptedAt(),
                delivery.getCollectedAt(),
                delivery.getDeliveredAt());
    }
}
