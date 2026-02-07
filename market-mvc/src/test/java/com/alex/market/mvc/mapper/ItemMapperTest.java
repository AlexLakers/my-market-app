package com.alex.market.mvc.mapper;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.projection.OrderItemsDetails;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringJUnitConfig
class ItemMapperTest {

    private static final Long VALID_ID=1L;
    private static final Integer ITEMS_COUNT=3;

    @Autowired
    private ItemMapper itemMapper;
    @Test
    void toDto() {
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        Item item = Item.builder().id(VALID_ID).title("testTitle1").price(1000L).description("testDesc1").imgPath("testImagePath1").build();

        ItemDto actual = itemMapper.toDto(item, Map.of(VALID_ID, 1));

        Assertions.assertThat(actual).isEqualTo(itemDto);
    }


    @Test
    void toDtoFromOrderItem() {
        Item item = Item.builder().id(VALID_ID).title("Apple iPhone 15").description("Latest iPhone model").imgPath("/images/iphone15.jpg").price(99900L).build();
        OrderItemsDetails orderItemDetails = new OrderItemsDetails(VALID_ID,item.getTitle(),item.getDescription(),item.getImgPath(),item.getPrice(),ITEMS_COUNT);

        ItemDto itemDto = itemMapper.toDtoFromOrderItemDetails(orderItemDetails);

        assertThat(itemDto).isNotNull();
        assertThat(itemDto.id()).isEqualTo(VALID_ID);
        assertThat(itemDto.title()).isEqualTo("Apple iPhone 15");
        assertThat(itemDto.description()).isEqualTo("Latest iPhone model");
        assertThat(itemDto.imgPath()).isEqualTo("/images/iphone15.jpg");
        assertThat(itemDto.price()).isEqualTo(99900L);
        assertThat(itemDto.count()).isEqualTo(ITEMS_COUNT);
    }
    @Test
    void toDtoFromCache_shouldReturnItemDto(){
        ItemCache itemCache=new ItemCache(VALID_ID,"title","desc",100L,"/images");
        Assertions.assertThat(itemMapper.toItemDtoFromCache(itemCache,3,"ImageAsBase64"))
                .isNotNull()
                .isInstanceOf(ItemDto.class)
                .hasFieldOrPropertyWithValue("id",VALID_ID)
                .hasFieldOrPropertyWithValue("title","title");
    }

    @TestConfiguration
    static class ItemMapperTestContextConfiguration {
        @Bean
        public ItemMapper itemMapper() {
            return new ItemMapperImpl();
        }
    }
}