package com.alex.market.mapper;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.model.Item;
import com.alex.market.model.Order;
import com.alex.market.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void toDto_shouldMapOrderCorrectlyWithSpringContext() {
        Item item1 = Item.builder().id(1L).title("Spring Test Item").price(1000L).build();
        Item item2 = Item.builder().id(2L).title("Another Item").price(2000L).build();
        Order order = Order.builder().id(200L).totalSum(5000L).build();

        order.addItem(item1, 2);
        order.addItem(item2, 1);

        OrderDto result = orderMapper.toDto(order);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(200L);
        assertThat(result.totalSum()).isEqualTo(5000L);
        assertThat(result.items()).hasSize(2);

        assertThat(result.items().get(0).id()).isEqualTo(1L);
        assertThat(result.items().get(0).price()).isEqualTo(1000L); // historyPrice
        assertThat(result.items().get(0).count()).isEqualTo(2);
    }

    @Test
    void toDto_shouldUseItemMapperForEachOrderItem() {
        Item item = Item.builder().id(5L).title("Test Item").description("Test Description").imgPath("/test.jpg").price(100L).build();
        Order order = Order.builder().id(201L).totalSum(500L).build();
        order.addItem(item, 1);
        order.addItem(item, 2);
        order.addItem(item, 3);

        OrderDto result = orderMapper.toDto(order);

        assertThat(result.items()).hasSize(3);

        assertThat(result.items())
                .allSatisfy(itemDto -> {
                    assertThat(itemDto.id()).isEqualTo(5L);
                    assertThat(itemDto.title()).isEqualTo("Test Item");
                    assertThat(itemDto.price()).isEqualTo(100L); // historyPrice
                });

        assertThat(result.items().get(0).count()).isEqualTo(1);
        assertThat(result.items().get(1).count()).isEqualTo(2);
        assertThat(result.items().get(2).count()).isEqualTo(3);
    }
    
    @TestConfiguration
    static class TestConfig {

        @Bean
        public ItemMapper itemMapper(){
            return new ItemMapperImpl();
        }
        @Bean
       public OrderMapper orderMapper() {
            return new OrderMapperImpl();
        }
    }
}