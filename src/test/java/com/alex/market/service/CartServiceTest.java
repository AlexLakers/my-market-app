package com.alex.market.service;

import com.alex.market.api.dto.input.CartChangeDto;
import com.alex.market.api.dto.output.CartDto;
import com.alex.market.api.dto.output.ItemDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.mapper.ItemMapperImpl;
import com.alex.market.model.CartAction;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.impl.CartServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringJUnitConfig
class CartServiceTest {

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;
    Map<Long,Integer> cartItemsCount;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID,2);

    }
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemMapper itemMapper;

    @ParameterizedTest
    @CsvSource({"PLUS, 3",
            "MINUS, 1",
    })
    void changeItemCount_shouldReturnIncrementCountItemInCartSuccess(CartAction action,Integer expectedCount) {
        CartChangeDto givenDto=new CartChangeDto(VALID_ID, action,cartItemsCount);
        Mockito.when(itemRepository.existsById(VALID_ID)).thenReturn(true);

        Integer actualCount=cartService.changeItemCount(givenDto);

        Assertions.assertThat(actualCount).isNotNull().isEqualTo(expectedCount);
    }

    @Test
    void changeItemCount_shouldThrowItemNotFoundException_whenItemIdNotExistsFail() {
        CartChangeDto givenDto=new CartChangeDto(INVALID_ID, CartAction.PLUS,cartItemsCount);
        Mockito.when(itemRepository.existsById(INVALID_ID)).thenReturn(false);

        Assertions.assertThatExceptionOfType(ItemNotFoundException.class)
                        .isThrownBy(()->cartService.changeItemCount(givenDto));
    }

    @Test
    void getItemsCartWithTotal_shouldReturnItemsCartSuccess() {
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID));
        Item expectedItem= new Item(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L,null);
        CartDto expectedDto=new CartDto(List.of(itemDto),2000L);
        Mockito.when(itemRepository.findAllById(Set.of(VALID_ID))).thenReturn(List.of(expectedItem));

        CartDto actualDto=cartService.getItemsCartWithTotal(cartItemsCount);

        Assertions.assertThat(actualDto).isNotNull().isEqualTo(expectedDto);
    }



    @TestConfiguration
    static class TestConfig {
        @Bean
        public ItemRepository itemRepository() {
            return Mockito.mock(ItemRepository.class);
        }

        @Bean
        public ItemMapper itemMapper() {
            return new ItemMapperImpl();
        }

        @Bean
        public CartService cartService() {
            return new CartServiceImpl(itemRepository(),itemMapper());
        }
    }
}