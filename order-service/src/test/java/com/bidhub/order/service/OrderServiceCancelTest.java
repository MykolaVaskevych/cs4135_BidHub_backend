package com.bidhub.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.order.exception.InvalidOrderStateException;
import com.bidhub.order.exception.OrderNotFoundException;
import com.bidhub.order.model.Order;
import com.bidhub.order.model.OrderStatus;
import com.bidhub.order.repository.DeliveryRepository;
import com.bidhub.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceCancelTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @InjectMocks private OrderService orderService;

    private static final UUID ORDER_ID = UUID.randomUUID();

    private Order awaitingCollectionOrder() {
        return Order.builder()
                .orderId(ORDER_ID)
                .auctionId(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .status(OrderStatus.AWAITING_COLLECTION)
                .build();
    }

    @Test
    @DisplayName("cancel: AWAITING_COLLECTION → CANCELLED")
    void cancel_awaitingCollection_succeeds() {
        Order order = awaitingCollectionOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.cancelOrder(ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED.name());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel: BEING_DELIVERED is rejected (only AWAITING_COLLECTION can cancel)")
    void cancel_beingDelivered_throws() {
        Order order = awaitingCollectionOrder();
        order.setStatus(OrderStatus.BEING_DELIVERED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("cancel: COMPLETED is rejected")
    void cancel_completed_throws() {
        Order order = awaitingCollectionOrder();
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("cancel: missing order returns 404 (OrderNotFoundException)")
    void cancel_missingOrder_throws() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
