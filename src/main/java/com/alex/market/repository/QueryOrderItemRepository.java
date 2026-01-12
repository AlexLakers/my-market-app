package com.alex.market.repository;

import com.alex.market.repository.projection.OrderItemsDetails;
import reactor.core.publisher.Flux;

public interface QueryOrderItemRepository {
    Flux<OrderItemsDetails> findItemsWithDetailsByOrderId(Long orderId);
}
