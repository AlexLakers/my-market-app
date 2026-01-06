package com.alex.market.service;

import com.alex.market.dto.output.OrderDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {
    Flux<OrderDto> findAllOrders();
    Mono<OrderDto> findOrderWithItems(Long orderId);
}
