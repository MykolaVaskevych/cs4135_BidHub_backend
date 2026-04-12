package com.bidhub.order.controller;

import com.bidhub.order.dto.*;
import com.bidhub.order.model.OrderStatus;
import com.bidhub.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(UUID.fromString(userId)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderResponse>> getCompletedOrders(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getCompletedOrders(UUID.fromString(userId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @GetMapping("/{orderId}/delivery")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getDeliveryByOrderId(orderId));
    }

    @PatchMapping("/{orderId}/confirm-collection")
    public ResponseEntity<OrderResponse> confirmCollection(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.confirmCollection(orderId));
    }

    @PatchMapping("/{orderId}/mark-delivered")
    public ResponseEntity<OrderResponse> markDelivered(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.markDelivered(orderId));
    }

    @PatchMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<OrderResponse> confirmDelivery(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.confirmDelivery(orderId));
    }
}
