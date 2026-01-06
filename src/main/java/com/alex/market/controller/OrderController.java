package com.alex.market.controller;

import com.alex.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/orders")
    public Mono<Rendering> getAllOrders() {
        return orderService.findAllOrders()
                .collectList()
                .map(orders -> Rendering
                        .view("orders")
                        .modelAttribute("orders", orders)
                        .status(HttpStatus.OK)
                        .build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrderById(@PathVariable("id") Long id,
                                        @RequestParam(defaultValue = "false") boolean newOrder) {
        return orderService.findOrderWithItems(id)
                .map(orderDto -> Rendering
                        .view("order")
                        .modelAttribute("order", orderDto)
                        .modelAttribute("newOrder", newOrder)
                        .status(HttpStatus.OK)
                        .build());
    }

    @PostMapping("/buy")
    public Mono<String> createOrder(@SessionAttribute Map<Long, Integer> cart) {

        return orderService.createOrder(cart)
                .map(orderDto -> {
                    cart.clear();
                    return "redirect:/orders/" + orderDto.id() + "?newOrder=true";
                });

    }


}
