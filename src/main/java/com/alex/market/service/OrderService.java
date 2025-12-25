package com.alex.market.service;



import com.alex.market.api.dto.output.OrderDto;
import com.alex.market.model.Order;

import java.util.List;
import java.util.Map;

public interface OrderService {
    OrderDto createOrder(Map<Long,Integer> cartItemsCounts);
}
