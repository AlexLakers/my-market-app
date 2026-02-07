package com.alex.market.mvc.repository;

import com.alex.market.mvc.repository.projection.OrderItemsDetails;
import reactor.core.publisher.Flux;

public interface QueryOrderItemRepository {
    Flux<OrderItemsDetails> findItemsWithDetailsByOrderId(Long orderId);
}
