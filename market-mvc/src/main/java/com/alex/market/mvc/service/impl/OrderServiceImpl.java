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
import com.alex.market.mvc.service.UserService;
import com.alex.market.mvc.service.CartService;
import com.alex.market.mvc.service.OrderService;
import com.alex.market.mvc.service.PaymentApiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final UserService userService;



    @PreAuthorize("hasAuthority('USER')")
    public Mono<OrderPaymentDto> createAndProcessOrderForAuthUser(Map<Long, Integer> cartItemsCounts) {
        log.info("Creating new order. Items in cart: {}", cartItemsCounts != null ? cartItemsCounts.size() : 0);

        return userService.getCurrentUserId()
                .flatMap(userId ->
                        cartService.getItemsCartWithCounts(cartItemsCounts)
                                .flatMap(itemsCount -> {
                                    log.debug("Getting {} positions items for order", itemsCount.size());

                                    Long totalSum = itemsCount.entrySet().stream()
                                            .mapToLong(entry -> entry.getKey().getPrice() * entry.getValue()).sum();
                                    log.info("Total sum of order: {}", totalSum);

                                    Order order = createNewOrder(userId, totalSum, OrderStatus.PENDING);

                                    return orderRepository.save(order)
                                            .flatMap(savedOrder -> {
                                                List<OrderItem> orderItems = itemsCount.entrySet().stream()
                                                        .map(entry -> createOrderItem(entry, savedOrder.getId()))
                                                        .collect(Collectors.toList());

                                                log.info("Saving {} order items for order id: {}", orderItems.size(), savedOrder.getId());

                                                return orderItemRepository.saveAll(orderItems)
                                                        .then(Mono.just(savedOrder));
                                            })
                                            .flatMap(this::processOrder)
                                            .map(processedOrder -> new OrderPaymentDto(
                                                    processedOrder.getId(),
                                                    processedOrder.getStatus().name()));
                                })
                                .doOnNext(dto -> log.info("Order created: id={}, status={}", dto.orderId(), dto.orderStatus()))
                                .doOnError(error -> log.error("Order creation failed", error))
                );
    }

    private Order createNewOrder(Long userId, Long totalSum, OrderStatus orderStatus) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalSum(totalSum);
        order.setStatus(orderStatus);
        return order;
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
    @PreAuthorize("hasAuthority('USER')")
    public Flux<OrderDto> findAllPaidOrdersForAuthUser() {
        log.info("Getting all orders");

        return userService.getCurrentUserId()
                .flatMapMany(userId ->
                        orderRepository.findAllByStatusAndUserId(OrderStatus.PAID, userId)
                                .flatMap(order ->
                                        orderItemRepository.findItemsWithDetailsByOrderId(order.getId())
                                                .collectList()
                                                .map(orderItems -> {
                                                    log.debug("For order with id: {} found {} items", order.getId(), orderItems.size());

                                                    List<ItemDto> itemDtos = orderItems.stream()
                                                            .map(itemMapper::toDtoFromOrderItemDetails)
                                                            .collect(Collectors.toList());

                                                    return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
                                                })
                                )
                                .doOnComplete(() -> log.debug("Finished handling orders"))
                                .doOnError(error -> log.error("Error during getting orders: {}", error.getMessage(), error))
                );
    }

    @Override
    @PreAuthorize("hasAuthority('USER')")
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
