package com.alex.market.service;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.mapper.ItemMapperImpl;
import com.alex.market.model.CartAction;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void changeItemCount_shouldReturnIncrementCountItemInCartSuccess(CartAction action, Integer expectedCount) {
        CartChangeDto givenDto=new CartChangeDto(VALID_ID, action,cartItemsCount);
        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));

        Integer actualCount=cartService.changeItemCount(givenDto).block();

        assertThat(actualCount).isNotNull().isEqualTo(expectedCount);
    }

    @Test
    void changeItemCount_shouldThrowItemNotFoundException_whenItemIdNotExistsFail() {
        CartChangeDto givenDto=new CartChangeDto(INVALID_ID, CartAction.PLUS,cartItemsCount);
        when(itemRepository.existsById(INVALID_ID)).thenReturn(Mono.just(false));

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(()->cartService.changeItemCount(givenDto).block());
    }

    @Test
    void getItemsCartWithTotal() {
    }

    @Test
    void getItemsCartWithCounts() {
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ItemRepository itemRepository() {
            return mock(ItemRepository.class);
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