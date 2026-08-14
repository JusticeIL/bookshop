package com.bookshop.order;

import com.bookshop.order.OrderDtos.CheckoutRequest;
import com.bookshop.order.OrderDtos.OrderDto;
import com.bookshop.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@AuthenticationPrincipal AuthenticatedUser user,
                             @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(user.id(), request);
    }

    @GetMapping
    public List<OrderDto> listOrders(@AuthenticationPrincipal AuthenticatedUser user) {
        return orderService.listOrders(user.id());
    }

    @GetMapping("/{id}")
    public OrderDto getOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return orderService.getOrder(user.id(), id);
    }
}
