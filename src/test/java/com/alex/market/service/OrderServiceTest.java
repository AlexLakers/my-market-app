package com.alex.market.service;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.exception.OrderNotFoundException;
import com.alex.market.mapper.OrderMapper;
import com.alex.market.model.Order;
import com.alex.market.repository.OrderRepository;
import com.alex.market.service.impl.OrderServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
class OrderServiceTest {

    private final Long VALID_ID=1L;
    private final Long INVALID_ID=1000000L;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderRepository orderRepository;

    private Map<Long, Integer> cartItemsCount;
    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        itemDto=new ItemDto(1L, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
    }
    @Test
    void createOrder_shouldCreateOrderAndReturnOrderDtoSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);
        when(orderRepository.save(any(Order.class))).thenReturn(new Order());

        OrderDto actualDto=orderService.createOrder(cartItemsCount);

        assertThat(actualDto).isNotNull().isEqualTo(orderDto);

    }

    @Test
    void getOrder_shouldReturnOrderDtoByIdSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);
        when(orderRepository.findById(VALID_ID)).thenReturn(Optional.of(new Order()));

        OrderDto actualDto= orderService.getOrder(VALID_ID);

        assertThat(actualDto).isNotNull().isEqualTo(orderDto);
    }

    @Test
    void getOrder_shouldReturnEmptyOptional_whenThrowOrderNotFoundExceptionFail() {
        OrderDto orderDto = new OrderDto(INVALID_ID, List.of(itemDto),1000L);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);
        when(orderRepository.findById(INVALID_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(()->orderService.getOrder(INVALID_ID));
    }

    @Test
    void getOrders_shouldReturnOrderDtoListSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);
        when(orderRepository.findAll()).thenReturn(List.of(new Order()));

        List<OrderDto> actualDto=orderService.getOrders();

        assertThat(actualDto).hasSize(1).contains(orderDto);
    }


    @TestConfiguration
    static class TestConfig {

        @Bean
        public OrderRepository orderRepository() {
            return mock(OrderRepository.class);
        }

        @Bean
        public OrderMapper orderMapper() {
            return mock(OrderMapper.class);
        }
        @Bean
        public CartService cartService() {
            return mock(CartService.class);
        }

        @Bean
        public OrderService orderService() {
            return new OrderServiceImpl(orderRepository(), cartService(),orderMapper());
        }
    }

}