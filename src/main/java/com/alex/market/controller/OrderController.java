package com.alex.market.controller;

import com.alex.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
   /* @GetMapping("/orders/{id}")
    public String getOrders(@PathVariable Long id,
                            @RequestParam(defaultValue = "false") boolean newOrder,
                            Model model) {
        model.addAttribute("order", orderService.getOrder(id));
        model.addAttribute("newOrder", newOrder);
        return "order";
    }*/

}
