package com.alex.market.mapper;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.model.Item;
import com.alex.market.model.Order;
import com.alex.market.model.OrderItem;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
class ItemMapperTest {

    @Autowired
    private ItemMapper itemMapper;
    @Test
    void toDto() {
        ItemDto itemDto = new ItemDto(1L, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        Item item = Item.builder().id(1L).title("testTitle1").price(1000L).description("testDesc1").imgPath("testImagePath1").build();

        ItemDto actual = itemMapper.toDto(item, Map.of(1L, 1));

        Assertions.assertThat(actual).isEqualTo(itemDto);
    }


    @Test
    void toDtoFromOrderItem() {

        Item item = Item.builder().id(1L).title("Apple iPhone 15").description("Latest iPhone model").imgPath("/images/iphone15.jpg").price(99900L).build();
        Order order = Order.builder().id(100L).totalSum(299700L).build();
        OrderItem orderItem = OrderItem.builder().id(50L).order(order).item(item).historyPrice(95000L).count(3).build();

        ItemDto itemDto = itemMapper.toDtoFromOrderItem(orderItem);

        assertThat(itemDto).isNotNull();
        assertThat(itemDto.id()).isEqualTo(1L);
        assertThat(itemDto.title()).isEqualTo("Apple iPhone 15");
        assertThat(itemDto.description()).isEqualTo("Latest iPhone model");
        assertThat(itemDto.imgPath()).isEqualTo("/images/iphone15.jpg");
        assertThat(itemDto.price()).isEqualTo(95000L); // Историческая цена, не 99900!
        assertThat(itemDto.count()).isEqualTo(3);
    }

    @TestConfiguration
    static class ItemMapperTestContextConfiguration {
        @Bean
        public ItemMapper itemMapper() {
            return new ItemMapperImpl();
        }
    }
}
