package com.alex.market.mvc.service.impl;


import com.alex.market.mvc.client.dto.PaymentRequest;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.OrderDto;
import com.alex.market.mvc.dto.output.OrderPaymentDto;
import com.alex.market.mvc.exception.OrderNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.model.Order;
import com.alex.market.mvc.model.OrderItem;
import com.alex.market.mvc.model.OrderStatus;
import com.alex.market.mvc.repository.OrderItemRepository;
import com.alex.market.mvc.repository.OrderRepository;
import com.alex.market.mvc.repository.projection.OrderItemsDetails;
import com.alex.market.mvc.service.CartService;
import com.alex.market.mvc.service.OrderService;
import com.alex.market.mvc.service.PaymentApiClientService;
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

    private static final Long GENERAL_ACCOUNT_ID = 1L;
    private static final Long GENERAL_USER_ID = 1L;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemMapper itemMapper;
    private final CartService cartService;
    private final PaymentApiClientService paymentApiClientService;

    public Mono<OrderPaymentDto> createAndProcessOrder(Map<Long, Integer> cartItemsCounts) {
        log.info("Creating new order.Items in cart: {}", cartItemsCounts != null ? cartItemsCounts.size() : 0);


        return cartService.getItemsCartWithCounts(cartItemsCounts)
                .flatMap(itemsCount -> {
                    log.debug("Getting {} positions items for order", itemsCount != null ? itemsCount.size() : 0);

                    Long totalSum = itemsCount.entrySet().stream()
                            .mapToLong(entry -> entry.getKey().getPrice() * entry.getValue()).sum();
                    log.info("Total sum of order: {}", totalSum);

                    Order order = new Order();
                    order.setStatus(OrderStatus.PENDING);
                    order.setTotalSum(totalSum);
                    order.setUserId(1L); //TODO
                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                log.debug("Order was saved in BD with id: {}", savedOrder.getId());

                                List<OrderItem> orderItems = itemsCount.entrySet().stream()
                                        .map(entry -> createOrderItem(entry, savedOrder.getId()))
                                        .collect(Collectors.toList());

                                log.info("Created {} positions for order with id: {}", orderItems.size(), savedOrder.getId());
                                return orderItemRepository.saveAll(orderItems)
                                        .then(Mono.just(savedOrder/*.getId()*/))
                                        .doOnSuccess(id ->
                                                log.info("Order created with id: {}", id)
                                        )
                                        .doOnError(error ->
                                                log.error("Failed to create order", error)
                                        ).flatMap(this::processOrder)
                                        .map(processedOrder -> new OrderPaymentDto(
                                                processedOrder.getId(),
                                                processedOrder.getStatus().name()))
                                        .doOnNext(dto->
                                                log.info("Order with id: {} is processed with status: {}",
                                                        dto.orderId(), dto.orderStatus()));
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

    private PaymentRequest createPaymentRequest(Order savedOrder) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(savedOrder.getId());
        paymentRequest.setAccountId(GENERAL_ACCOUNT_ID);
        paymentRequest.amount(savedOrder.getTotalSum());
        paymentRequest.setUserId(GENERAL_USER_ID);
        return paymentRequest;
    }

    private Mono<Order> processOrder(Order savedOrder) {
        PaymentRequest paymentRequest = createPaymentRequest(savedOrder);

        return paymentApiClientService.processPaymentInTransaction(paymentRequest)
                .flatMap(paymentResult -> {

                    OrderStatus orderStatus = switch (paymentResult.status()) {

                        case SUCCESS -> {
                            log.info("Payment successful for order with id: {}", savedOrder.getId());

                            yield OrderStatus.PAID;
                        }

                        case BUSINESS_FAIL -> {
                            log.warn("Payment failed for reason: {} for order with id: {}", paymentResult.failureReason(), savedOrder.getId());

                            yield OrderStatus.FAILED;
                        }

                        case NETWORK_ERROR, SERVICE_ERROR -> OrderStatus.PENDING;
                    };

                    savedOrder.setStatus(orderStatus);

                    return orderRepository.save(savedOrder)
                            .doOnNext(result ->
                                    log.debug("Order with id: {} status updated to status: {}", savedOrder.getId(), orderStatus));
                });
    }


    @Override
    public Flux<OrderDto> findAllPaidOrders() {
        log.info("Getting all orders");

        return orderRepository.findAllByStatus(OrderStatus.PAID)
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
