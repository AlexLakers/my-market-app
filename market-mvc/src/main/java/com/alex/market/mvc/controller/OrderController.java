package com.alex.market.mvc.controller;

import com.alex.market.mvc.model.OrderStatus;
import com.alex.market.mvc.security.service.UserService;
import com.alex.market.mvc.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/orders")
    public Mono<Rendering> getAllOrdersForUser() {
        return userService.getCurrentUserId()
                .flatMap(userId -> orderService.findAllPaidOrdersByUserId(userId)
                        .collectList()
                        .map(orders -> Rendering
                                .view("orders")
                                .modelAttribute("orders", orders)
                                .status(HttpStatus.OK)
                                .build()));
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrderByIdForUser(@PathVariable("id") Long id,
                                               @RequestParam(defaultValue = "false") boolean newOrder) {
        log.info("---endpoint 'getOrderById' with input params: newOrder:{} and id:{} was started---", newOrder, id);

        return userService.getCurrentUserId()
                .flatMap(userId -> orderService.findOrderWithItemsByUserId(id, userId)
                        .map(orderDto -> Rendering
                                .view("order")
                                .modelAttribute("order", orderDto)
                                .modelAttribute("newOrder", newOrder)
                                .status(HttpStatus.OK)
                                .build()));
    }

    @PostMapping("/buy")
    public Mono<String> createOrderForUser(@SessionAttribute(required = false) Map<Long, Integer> cart) {
        log.info("---endpoint 'createOrder' with cart:{} from session was started---", cart);

        return userService.getCurrentUserId()
                .flatMap(userId -> orderService.createAndProcessOrderByUserId(cart, userId)
                        .map(dto -> {
                            if (dto.orderStatus().equals(OrderStatus.PAID.name())) {
                                cart.clear();
                                return "redirect:/orders/" + dto.orderId() + "?newOrder=true";
                            } else return "redirect:/cart/items" + "?paymentOrderStatus=" + dto.orderStatus();
                        }));
    }
}
