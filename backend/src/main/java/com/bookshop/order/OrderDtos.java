package com.bookshop.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Request/response contracts for the checkout/orders API. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /**
     * Checkout request. Payment is intentionally mocked: we accept a free-text
     * "card number" but never validate, store, or transmit it anywhere.
     */
    public record CheckoutRequest(
            @NotBlank @Size(max = 120) String shippingName,
            @NotBlank @Size(max = 500) String shippingAddress,
            @Size(max = 30) String mockCardNumber) {
    }

    public record OrderItemDto(Long bookId, String title, BigDecimal unitPrice, int quantity) {

        static OrderItemDto from(OrderItem item) {
            return new OrderItemDto(item.getBookId(), item.getTitle(), item.getUnitPrice(), item.getQuantity());
        }
    }

    public record OrderDto(
            Long id,
            String status,
            BigDecimal totalAmount,
            String shippingName,
            String shippingAddress,
            String paymentReference,
            OffsetDateTime createdAt,
            List<OrderItemDto> items) {

        static OrderDto from(Order order) {
            return new OrderDto(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getShippingName(),
                    order.getShippingAddress(),
                    order.getPaymentReference(),
                    order.getCreatedAt(),
                    order.getItems().stream().map(OrderItemDto::from).toList());
        }
    }
}
