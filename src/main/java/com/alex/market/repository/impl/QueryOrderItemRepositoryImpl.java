package com.alex.market.repository.impl;

import com.alex.market.repository.QueryOrderItemRepository;
import com.alex.market.repository.projection.OrderItemsDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class QueryOrderItemRepositoryImpl implements QueryOrderItemRepository {
    private final DatabaseClient databaseClient;

    @Override
    public Flux<OrderItemsDetails> findItemsWithDetailsByOrderId(Long orderId) {

        return databaseClient.sql("""
                                SELECT
                                    i.*,
                                    oi.history_price,
                                    oi.count
                                FROM items i
                                JOIN orders_items oi ON i.id = oi.item_id
                                WHERE oi.order_id = :orderId
                                """)
                .bind("orderId", orderId)
                .fetch()
                .all()
                .map(row -> new OrderItemsDetails(
                        (Long) row.get("id"),
                        (String) row.get("title"),
                        (String) row.get("description"),
                        (String) row.get("img_path"),
                        (Long) row.get("history_price"),
                        (Integer) row.get("count")
                ));
    }
}
