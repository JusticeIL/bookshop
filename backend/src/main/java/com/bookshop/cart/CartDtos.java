package com.bookshop.cart;

import com.bookshop.book.BookDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Request/response contracts for the cart API. */
public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(
            @NotNull Long bookId,
            @Min(1) int quantity) {
    }

    public record UpdateQuantityRequest(
            @Min(1) int quantity) {
    }

    public record CartItemDto(BookDto book, int quantity, BigDecimal lineTotal) {

        static CartItemDto from(CartItem item) {
            BookDto book = BookDto.from(item.getBook());
            return new CartItemDto(
                    book,
                    item.getQuantity(),
                    book.price().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }

    public record CartDto(List<CartItemDto> items, int totalItems, BigDecimal totalAmount) {

        static CartDto from(List<CartItem> items) {
            List<CartItemDto> dtos = items.stream().map(CartItemDto::from).toList();
            int totalItems = dtos.stream().mapToInt(CartItemDto::quantity).sum();
            BigDecimal totalAmount = dtos.stream()
                    .map(CartItemDto::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartDto(dtos, totalItems, totalAmount);
        }
    }
}
