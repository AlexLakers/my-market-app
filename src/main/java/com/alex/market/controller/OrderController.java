package com.alex.market.controller;

import com.alex.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/orders")
    public Mono<Rendering> getAllOrders() {
        return orderService.findAllOrders()
                .collectList()
                .map(orders -> Rendering.view("orders").modelAttribute("orders", orders)
                        .build());
    }

}
