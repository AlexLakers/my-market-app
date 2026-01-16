package com.alex.market.mvc.repository;

import com.alex.market.mvc.model.Order;
import com.alex.market.mvc.model.OrderStatus;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
    Flux<Order> findAllByStatus(OrderStatus status);
}
