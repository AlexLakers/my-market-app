package com.alex.market.repository;

import com.alex.market.model.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
}
