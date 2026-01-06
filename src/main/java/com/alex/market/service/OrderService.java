package com.alex.market.service;

import com.alex.market.dto.output.OrderDto;
import reactor.core.publisher.Flux;

public interface OrderService {
    Flux<OrderDto> findAllOrders();
}
