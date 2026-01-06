package com.alex.market.service.impl;


import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.exception.OrderNotFoundException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.model.Item;
import com.alex.market.model.Order;
import com.alex.market.model.OrderItem;
import com.alex.market.repository.OrderItemRepository;
import com.alex.market.repository.OrderRepository;
import com.alex.market.repository.projection.OrderItemsDetails;
import com.alex.market.service.CartService;
import com.alex.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemMapper itemMapper;
    private final CartService cartService;

    public Mono<Long> createOrder(Map<Long, Integer> cartItemsCounts) {
        return cartService.getItemsCartWithCounts(cartItemsCounts)
                .flatMap(itemsCount -> {

                    Long totalSum = itemsCount.entrySet().stream()
                            .mapToLong(entry -> entry.getKey().getPrice() * entry.getValue()).sum();

                    Order order = new Order();
                    order.setTotalSum(totalSum);
                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                List<OrderItem> orderItems = itemsCount.entrySet().stream()
                                        .map(entry -> createOrderItem(entry, savedOrder.getId()))
                                        .collect(Collectors.toList());

                                return orderItemRepository.saveAll(orderItems)
                                        .then(Mono.just(savedOrder.getId()))
                                        .doOnSuccess(idd -> log.info("Order created: {}", idd))
                                        .doOnError(error -> log.error("Failed to create order", error));
                            });
                });
    }

    private OrderItem createOrderItem(Map.Entry<Item, Integer> entry, Long orderId) {
        return OrderItem.builder()
                .orderId(orderId)
                .itemId(entry.getKey().getId())
                .historyPrice(entry.getKey().getPrice())
                .count(entry.getValue())
                .build();
    }


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

    @Override
    public Mono<OrderDto> findOrderWithItems(Long orderId) {
        return Mono.zip(
                orderRepository.findById(orderId)
                        .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId))),

                orderItemRepository.findItemsWithDetailsByOrderId(orderId)
                        .collectList()

        ).map(tuple -> {
            Order order = tuple.getT1();

            List<OrderItemsDetails> orderItems = tuple.getT2();

            List<ItemDto> itemsDto = orderItems.stream()
                    .map(itemMapper::toDtoFromOrderItemDetails)
                    .collect(Collectors.toList());

            return new OrderDto(order.getId(), itemsDto, order.getTotalSum());
        });
    }
}
