package com.alex.market.service.impl;

import com.alex.market.api.dto.output.ItemDto;
import com.alex.market.api.dto.output.OrderDto;
import com.alex.market.exception.OrderNotFoundException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.mapper.OrderMapper;
import com.alex.market.model.Item;
import com.alex.market.model.Order;
import com.alex.market.model.OrderItem;
import com.alex.market.repository.OrderRepository;
import com.alex.market.service.CartService;
import com.alex.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final OrderMapper orderMapper;

    @Override
    public OrderDto createOrder(Map<Long, Integer> cartItemsCounts) {
        Map<Item, Integer> itemsCounts = cartService.getItemsCartWithCounts(cartItemsCounts);

        Order order = new Order();

        itemsCounts.forEach(order::addItem);

        order.setTotalSum(
                order.getOrderItems().stream()
                        .mapToLong(oi -> oi.getHistoryPrice() * oi.getCount())
                        .sum()
        );

        return orderMapper.toDto(orderRepository.save(order));
    }


    @Override
    public OrderDto getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(orderMapper::toDto).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public List<OrderDto> getOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toDto).collect(Collectors.toList());
    }

}
