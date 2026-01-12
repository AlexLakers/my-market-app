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
        log.info("Creating new order.Items in cart: {}", cartItemsCounts != null ? cartItemsCounts.size() : 0);

        return cartService.getItemsCartWithCounts(cartItemsCounts)
                .flatMap(itemsCount -> {
                    log.debug("Getting {} positions items for order заказа", itemsCount != null ? itemsCount.size() : 0);

                    Long totalSum = itemsCount.entrySet().stream()
                            .mapToLong(entry -> entry.getKey().getPrice() * entry.getValue()).sum();
                    log.info("Total sum or order: {}", totalSum);

                    Order order = new Order();
                    order.setTotalSum(totalSum);
                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                log.debug("Order was saved in BD with id: {}", savedOrder.getId());

                                List<OrderItem> orderItems = itemsCount.entrySet().stream()
                                        .map(entry -> createOrderItem(entry, savedOrder.getId()))
                                        .collect(Collectors.toList());

                                log.info("Created {} positions for order with id={}", orderItems.size(), savedOrder.getId());
                                return orderItemRepository.saveAll(orderItems)
                                        .then(Mono.just(savedOrder.getId()))
                                        .doOnSuccess(id ->
                                                log.info("Order created with id: {}", id)
                                        )
                                        .doOnError(error ->
                                                log.error("Failed to create order", error)
                                        );
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
        log.info("Getting all orders");

        return orderRepository.findAll()
                .flatMap(order ->
                        orderItemRepository.findItemsWithDetailsByOrderId(order.getId())
                                .collectList()
                                .map(orderItems -> {
                                    log.info("Found orders: {}", orderItems.size());

                                    List<ItemDto> itemDtos = orderItems.stream()
                                            .map(itemMapper::toDtoFromOrderItemDetails)
                                            .collect(Collectors.toList());

                                    log.debug("For order with id: {} found {} items", order.getId(), itemDtos.size());
                                    return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
                                }))
                .doOnComplete(() ->
                        log.debug("Finished handling orders")
                )
                .doOnError(error ->
                        log.error("Error during getting orders: {}", error.getMessage(), error)
                );
    }

    @Override
    public Mono<OrderDto> findOrderWithItems(Long orderId) {
        log.info("Getting order with id: {} with items", orderId);

        return Mono.zip(
                        orderRepository.findById(orderId)
                                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId))),

                        orderItemRepository.findItemsWithDetailsByOrderId(orderId)
                                .collectList()
                                .doOnNext(orderItems ->
                                        log.debug("Found {} items for order wit id: {}", orderItems.size(), orderId)
                                )

                ).map(tuple -> {
                    Order order = tuple.getT1();

                    List<OrderItemsDetails> orderItems = tuple.getT2();

                    List<ItemDto> itemsDto = orderItems.stream()
                            .map(itemMapper::toDtoFromOrderItemDetails)
                            .collect(Collectors.toList());

                    log.info("Order with id: {} retrieved, total: {}, items: {}", orderId, order.getTotalSum(), itemsDto.size());
                    return new OrderDto(order.getId(), itemsDto, order.getTotalSum());
                })
                .doOnError(OrderNotFoundException.class, error ->
                        log.warn("Order not found with id: {}", orderId)
                )
                .doOnError(error ->
                        log.error("Error getting order wit id:{}: {}", orderId, error.getMessage())
                );
    }
}
