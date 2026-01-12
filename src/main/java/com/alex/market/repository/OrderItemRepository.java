package com.alex.market.repository;

import com.alex.market.model.OrderItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long>, QueryOrderItemRepository {
}
