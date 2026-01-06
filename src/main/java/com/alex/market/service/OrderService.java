package com.alex.market.service;

import com.alex.market.dto.output.OrderDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface OrderService {
    Flux<OrderDto> findAllOrders();
    Mono<OrderDto> findOrderWithItems(Long orderId);
    Mono<Long> createOrder(Map<Long, Integer> cartItemsCounts);
}
