package com.alex.market.service.impl;

import com.alex.market.api.dto.output.ItemDto;
import com.alex.market.api.dto.output.OrderDto;
import com.alex.market.exception.OrderNotFoundException;
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

        Order savedOrder = orderRepository.save(order);
        return toOrderDto(savedOrder);
    }


    private OrderDto toOrderDto(Order order) {
        List<ItemDto> itemDtos = order.getOrderItems().stream()
                .map(this::toItemDtoFromOrderItem)
                .collect(Collectors.toList());

        return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
    }

    private ItemDto toItemDtoFromOrderItem(OrderItem orderItem) {
        Item item = orderItem.getItem();
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                orderItem.getHistoryPrice(),
                orderItem.getCount()
        );
    }

}
