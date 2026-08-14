package com.bookshop.cart;

import com.bookshop.book.Book;
import com.bookshop.book.BookRepository;
import com.bookshop.cart.CartDtos.CartDto;
import com.bookshop.common.BadRequestException;
import com.bookshop.common.NotFoundException;
import com.bookshop.user.User;
import com.bookshop.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository,
                       BookRepository bookRepository,
                       UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartDto getCart(Long userId) {
        return CartDto.from(cartItemRepository.findByUserIdOrderByIdAsc(userId));
    }

    @Transactional
    public CartDto addItem(Long userId, Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book %d not found".formatted(bookId)));

        cartItemRepository.findByUserIdAndBookId(userId, bookId).ifPresentOrElse(
                item -> item.setQuantity(requireInStock(book, item.getQuantity() + quantity)),
                () -> {
                    User user = userRepository.getReferenceById(userId);
                    cartItemRepository.save(new CartItem(user, book, requireInStock(book, quantity)));
                });
        return getCart(userId);
    }

    @Transactional
    public CartDto updateQuantity(Long userId, Long bookId, int quantity) {
        cartItemRepository.findByUserIdAndBookId(userId, bookId)
                .map(item -> {
                    item.setQuantity(requireInStock(item.getBook(), quantity));
                    return item;
                })
                .orElseThrow(() -> new NotFoundException("Book %d is not in the cart".formatted(bookId)));
        return getCart(userId);
    }

    /** Validates the requested quantity against stock; returns it for fluent use. */
    private static int requireInStock(Book book, int quantity) {
        if (quantity > book.getStock()) {
            throw new BadRequestException(
                    "Only %d copies of '%s' are in stock".formatted(book.getStock(), book.getTitle()));
        }
        return quantity;
    }

    @Transactional
    public CartDto removeItem(Long userId, Long bookId) {
        cartItemRepository.deleteByUserIdAndBookId(userId, bookId);
        return getCart(userId);
    }
}
