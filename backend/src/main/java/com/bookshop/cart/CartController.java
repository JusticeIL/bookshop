package com.bookshop.cart;

import com.bookshop.cart.CartDtos.AddItemRequest;
import com.bookshop.cart.CartDtos.CartDto;
import com.bookshop.cart.CartDtos.UpdateQuantityRequest;
import com.bookshop.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartDto getCart(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.id());
    }

    @PostMapping("/items")
    public CartDto addItem(@AuthenticationPrincipal AuthenticatedUser user,
                           @Valid @RequestBody AddItemRequest request) {
        return cartService.addItem(user.id(), request.bookId(), request.quantity());
    }

    @PutMapping("/items/{bookId}")
    public CartDto updateQuantity(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable Long bookId,
                                  @Valid @RequestBody UpdateQuantityRequest request) {
        return cartService.updateQuantity(user.id(), bookId, request.quantity());
    }

    @DeleteMapping("/items/{bookId}")
    public CartDto removeItem(@AuthenticationPrincipal AuthenticatedUser user,
                              @PathVariable Long bookId) {
        return cartService.removeItem(user.id(), bookId);
    }
}
