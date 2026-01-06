package com.alex.market.service;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.mapper.ItemMapperImpl;
import com.alex.market.model.Order;
import com.alex.market.repository.OrderItemRepository;
import com.alex.market.repository.OrderRepository;
import com.alex.market.repository.projection.OrderItemsDetails;
import com.alex.market.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class OrderServiceTest {
    private final Long VALID_ID=1L;
    private final Long INVALID_ID=1000000L;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderItemRepository orderItemRepository;
   /* @Autowired
    private OrderMapper orderMapper;*/
    @Autowired
    private OrderRepository orderRepository;

    private Map<Long, Integer> cartItemsCount;
    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        itemDto=new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
    }

    @Test
    void getAllOrders_shouldReturnOrderDtoListSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        List<OrderItemsDetails> listDetails=List.of(new OrderItemsDetails(VALID_ID,itemDto.title(),itemDto.description(),itemDto.imgPath(),itemDto.price(),itemDto.count()));
       // when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);
        when(orderRepository.findAll()).thenReturn(Flux.fromIterable(List.of(Order.builder().id(VALID_ID).totalSum(1000L).build())));
        when(orderItemRepository.findItemsWithDetailsByOrderId(VALID_ID)).thenReturn(Flux.fromIterable(listDetails));

        List<OrderDto> actualDto=orderService.findAllOrders().collectList().block();

        assertThat(actualDto).hasSize(1).contains(orderDto);
    }

    @TestConfiguration
    static class OrderServiceTestContextConfiguration {
        @Bean
        public OrderService orderService(OrderRepository orderRepository,OrderItemRepository orderItemRepository,ItemMapper itemMapper) {
            return new OrderServiceImpl(orderRepository,orderItemRepository,itemMapper);
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
            return new ItemMapperImpl();
        }
    }

}