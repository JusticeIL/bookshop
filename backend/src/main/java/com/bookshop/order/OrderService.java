package com.bookshop.order;

import com.bookshop.book.Book;
import com.bookshop.book.BookRepository;
import com.bookshop.cart.CartItem;
import com.bookshop.cart.CartItemRepository;
import com.bookshop.common.BadRequestException;
import com.bookshop.common.NotFoundException;
import com.bookshop.config.CacheConfig;
import com.bookshop.order.OrderDtos.CheckoutRequest;
import com.bookshop.order.OrderDtos.OrderDto;
import com.bookshop.user.User;
import com.bookshop.user.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    /** Buyers may cancel a confirmed order for this long after purchase. */
    static final Duration CANCELLATION_WINDOW = Duration.ofHours(24);

    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        UserRepository userRepository,
                        BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    /**
     * Converts the active cart into an order atomically:
     * validates stock under row locks, decrements it, snapshots prices, empties
     * the cart. Payment is mocked with a generated confirmation reference.
     * Stock changed, so both catalog caches are evicted.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.BOOKS_PAGE_CACHE, CacheConfig.BOOK_CACHE}, allEntries = true)
    public OrderDto checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = Optional.of(cartItemRepository.findByUserIdOrderByIdAsc(userId))
                .filter(items -> !items.isEmpty())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        BigDecimal total = cartItems.stream()
                .map(item -> item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // The recipient is always the account owner; their full name was
        // validated at registration time.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Order order = new Order(user, total, user.getDisplayName(),
                request.shippingAddress().trim(), mockPaymentReference());

        cartItems.stream()
                .map(cartItem -> toOrderItem(order, cartItem))
                .forEach(order::addItem);

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);
        return OrderDto.from(saved);
    }

    /**
     * Cancels a confirmed order within {@link #CANCELLATION_WINDOW} of purchase,
     * returning every line's quantity back to stock. History is preserved:
     * the row flips to CANCELLED rather than being deleted.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.BOOKS_PAGE_CACHE, CacheConfig.BOOK_CACHE}, allEntries = true)
    public OrderDto cancel(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order %d not found".formatted(orderId)));

        Optional.of(order)
                .filter(o -> STATUS_CONFIRMED.equals(o.getStatus()))
                .orElseThrow(() -> new BadRequestException(
                        "Order #%d is already %s".formatted(orderId, order.getStatus().toLowerCase())));
        Optional.of(order)
                .filter(o -> o.getCreatedAt().plus(CANCELLATION_WINDOW).isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new BadRequestException(
                        "Orders can only be cancelled within 24 hours of purchase"));

        // Restore stock under row locks (mirrors the checkout path).
        order.getItems().forEach(item ->
                bookRepository.findByIdForUpdate(item.getBookId())
                        .ifPresent(book -> book.increaseStock(item.getQuantity())));
        order.markCancelled();
        return OrderDto.from(order);
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

    private OrderItem toOrderItem(Order order, CartItem cartItem) {
        // Re-read the book row with SELECT ... FOR UPDATE so concurrent
        // checkouts serialize on it - overselling is impossible even under
        // simultaneous purchases of the last copy.
        Book book = bookRepository.findByIdForUpdate(cartItem.getBook().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Book %d not found".formatted(cartItem.getBook().getId())));
        if (cartItem.getQuantity() > book.getStock()) {
            throw new BadRequestException(
                    "Only %d copies of '%s' are in stock".formatted(book.getStock(), book.getTitle()));
        }
        book.decreaseStock(cartItem.getQuantity());
        return new OrderItem(order, book.getId(), book.getTitle(), book.getPrice(), cartItem.getQuantity());
    }

    private static String mockPaymentReference() {
        return "PAY-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
