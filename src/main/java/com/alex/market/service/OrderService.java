package com.alex.market.service;


import com.alex.market.dto.output.OrderDto;

import java.util.List;
import java.util.Map;

public interface OrderService {
    OrderDto createOrder(Map<Long, Integer> cartItemsCounts);

    OrderDto getOrder(Long orderId);

    List<OrderDto> getOrders();
}
