package com.alex.market.service;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.exception.OrderNotFoundException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.model.Order;
import com.alex.market.repository.OrderItemRepository;
import com.alex.market.repository.OrderRepository;
import com.alex.market.repository.projection.OrderItemsDetails;
import com.alex.market.service.impl.OrderServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class OrderServiceTest {
    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = 1000000L;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ItemMapper itemMapper;

    /* @Autowired
     private OrderMapper orderMapper;*/
    @Autowired
    private OrderRepository orderRepository;

    private Map<Long, Integer> cartItemsCount;
    private ItemDto itemDto;
    private OrderItemsDetails orderItemsDetails;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        orderItemsDetails = new OrderItemsDetails(VALID_ID, itemDto.title(), itemDto.description(), itemDto.imgPath(), itemDto.price(), itemDto.count());
    }

    @Test
    void getAllOrders_shouldReturnOrderDtoListSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto), 1000L);
        when(orderRepository.findAll()).thenReturn(Flux.fromIterable(List.of(Order.builder().id(VALID_ID).totalSum(1000L).build())));
        when(orderItemRepository.findItemsWithDetailsByOrderId(VALID_ID)).thenReturn(Flux.fromIterable(List.of(orderItemsDetails)));
        when(itemMapper.toDtoFromOrderItemDetails(orderItemsDetails)).thenReturn(itemDto);

        List<OrderDto> actualDto = orderService.findAllOrders().collectList().block();

        assertThat(actualDto).hasSize(1).contains(orderDto);
    }

    @Test
    void findOrderWithItems_shouldReturnOrderDtoByIdSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto), 1000L);
        Order order = Order.builder().id(VALID_ID).totalSum(1000L).build();
        when(orderRepository.findById(VALID_ID)).thenReturn(Mono.just(order));
        when(orderItemRepository.findItemsWithDetailsByOrderId(VALID_ID)).thenReturn(Flux.fromIterable(List.of(orderItemsDetails)));
        when(itemMapper.toDtoFromOrderItemDetails(orderItemsDetails)).thenReturn(itemDto);

        OrderDto actualOrderDto = orderService.findOrderWithItems(VALID_ID).block();

        Assertions.assertThat(actualOrderDto).isEqualTo(orderDto);
    }

    @Test
    void findOrderWithItems_shouldThrowOrderNotFoundException_whenMonoIsEmptyFail() {

        when(orderRepository.findById(INVALID_ID)).thenReturn(Mono.empty());
        when(orderItemRepository.findItemsWithDetailsByOrderId(INVALID_ID)).thenReturn(Flux.fromIterable(List.of(orderItemsDetails)));

        Assertions.assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.findOrderWithItems(INVALID_ID).block());
    }
@Test
void createOrder_shouldCreateOrderAndReturnOrderDtoSuccess(){

}

/*    @Test
    void createOrder_shouldCreateOrderAndReturnOrderDtoSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);
        when(orderRepository.save(any(Order.class))).thenReturn(new Order());

        OrderDto actualDto=orderService.createOrder(cartItemsCount);

        assertThat(actualDto).isNotNull().isEqualTo(orderDto);

    }*/
   /* public Mono<OrderDto> createOrder(Map<Long, Integer> cartItemsCounts) {
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
                                        .collectList()
                                        .map(savedOrderItems -> orderMapper.toDto(savedOrder, savedOrderItems, itemsCount.keySet().stream().toList()))
                                        .doOnSuccess(dto -> log.info("Order created: {}", dto.id()))
                                        .doOnError(error -> log.error("Failed to create order", error));
                            });
                });
    }*/

    @TestConfiguration
    static class OrderServiceTestContextConfiguration {
        @Bean
        public CartService cartService() {
            return Mockito.mock(CartService.class);
        }

        @Bean
        public OrderService orderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ItemMapper itemMapper, CartService cartService) {
            return new OrderServiceImpl(orderRepository, orderItemRepository, itemMapper, cartService);
        }

        @Bean
        public OrderRepository orderRepository() {
            return Mockito.mock(OrderRepository.class);
        }

        @Bean
        public OrderItemRepository orderItemRepository() {
            return Mockito.mock(OrderItemRepository.class);
        }

        @Bean
        public ItemMapper itemMapper() {
            return Mockito.mock(ItemMapper.class);
        }
    }

}