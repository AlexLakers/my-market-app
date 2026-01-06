package com.alex.market.service.impl;


import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.repository.OrderItemRepository;
import com.alex.market.repository.OrderRepository;
import com.alex.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemMapper itemMapper;

    @Override
    public Flux<OrderDto> findAllOrders() {
        return orderRepository.findAll()
                .flatMap(order ->
                        orderItemRepository.findItemsWithDetailsByOrderId(order.getId())
                                .collectList()
                                .map(orderItems -> {
                                    List<ItemDto> itemDtos = orderItems.stream()
                                            .map(itemMapper::toDtoFromOrderItemDetails)
                                            .collect(Collectors.toList());

                                    return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
                                }));
    }
}
