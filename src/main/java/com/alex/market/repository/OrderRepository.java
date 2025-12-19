package com.alex.market.repository;


import com.alex.market.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(
            attributePaths = Order.Fields.items,
            type = EntityGraph.EntityGraphType.FETCH)
    List<Order> findAllWithItems();
}
