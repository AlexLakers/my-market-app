package com.alex.market.service;

import com.alex.market.api.dto.CartChangeDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.model.CartAction;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
class CartServiceTest {

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;
    Map<Long,Integer> cartItemsCount;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID,2);
        cartItemsCount.put(2L,3);

    }
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartService cartService;

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



    @TestConfiguration
    static class TestConfig {
        @Bean
        public ItemRepository itemRepository() {
            return Mockito.mock(ItemRepository.class);
        }

        @Bean
        public CartService cartService() {
            return new CartServiceImpl(itemRepository());
        }
    }
}