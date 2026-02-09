package com.alex.market.mvc.service;

import com.alex.market.mvc.dto.output.OrderDto;
import com.alex.market.mvc.dto.output.OrderPaymentDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface OrderService {
    Flux<OrderDto> findAllPaidOrdersByUserId(Long userId);
    Mono<OrderDto> findOrderWithItems(Long orderId);
    Mono<OrderPaymentDto> createAndProcessOrderByUserId(Map<Long, Integer> cartItemsCounts,Long userId);
}
