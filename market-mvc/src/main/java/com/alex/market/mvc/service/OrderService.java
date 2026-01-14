package com.alex.market.mvc.service;

import com.alex.market.mvc.dto.output.OrderDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface OrderService {
    Flux<OrderDto> findAllOrders();
    Mono<OrderDto> findOrderWithItems(Long orderId);
    Mono<Long> createOrder(Map<Long, Integer> cartItemsCounts);
}
