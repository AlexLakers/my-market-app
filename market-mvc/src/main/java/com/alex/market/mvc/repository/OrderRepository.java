package com.alex.market.mvc.repository;

import com.alex.market.mvc.model.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
}
