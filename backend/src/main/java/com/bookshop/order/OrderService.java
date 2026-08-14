package com.bookshop.order;

import com.bookshop.book.Book;
import com.bookshop.cart.CartItem;
import com.bookshop.cart.CartItemRepository;
import com.bookshop.common.BadRequestException;
import com.bookshop.common.NotFoundException;
import com.bookshop.order.OrderDtos.CheckoutRequest;
import com.bookshop.order.OrderDtos.OrderDto;
import com.bookshop.user.User;
import com.bookshop.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Converts the active cart into an order atomically:
     * validates stock, decrements it, snapshots prices, empties the cart.
     * Payment is mocked with a generated confirmation reference.
     */
    @Transactional
    public OrderDto checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal total = cartItems.stream()
                .map(item -> item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        User user = userRepository.getReferenceById(userId);
        String paymentReference = "PAY-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(user, total, request.shippingName().trim(),
                request.shippingAddress().trim(), paymentReference);

        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            if (cartItem.getQuantity() > book.getStock()) {
                throw new BadRequestException(
                        "Only %d copies of '%s' are in stock".formatted(book.getStock(), book.getTitle()));
            }
            book.decreaseStock(cartItem.getQuantity());
            order.addItem(new OrderItem(order, book.getId(), book.getTitle(),
                    book.getPrice(), cartItem.getQuantity()));
        }

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);
        return OrderDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderDto::from)
                .orElseThrow(() -> new NotFoundException("Order %d not found".formatted(orderId)));
    }
}
