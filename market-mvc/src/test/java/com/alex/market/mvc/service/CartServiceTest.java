package com.alex.market.mvc.service;

import com.alex.market.mvc.client.api.DefaultApi;
import com.alex.market.mvc.client.dto.AccountResponse;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.output.AccountBalanceDto;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.mapper.ItemMapperImpl;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class CartServiceTest {
    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;
    Map<Long, Integer> cartItemsCount;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);

    }

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private DefaultApi paymentApi;

    @ParameterizedTest
    @CsvSource({"PLUS, 3",
            "MINUS, 1",
    })
    void changeItemCount_shouldReturnIncrementCountItemInCartSuccess(CartAction action, Integer expectedCount) {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, action, cartItemsCount);
        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));

        Integer actualCount = cartService.changeItemCount(givenDto).block();

        assertThat(actualCount).isNotNull().isEqualTo(expectedCount);
    }

    @Test
    void changeItemCount_shouldThrowItemNotFoundException_whenItemIdNotExistsFail() {
        CartChangeDto givenDto = new CartChangeDto(INVALID_ID, CartAction.PLUS, cartItemsCount);
        when(itemRepository.existsById(INVALID_ID)).thenReturn(Mono.just(false));

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> cartService.changeItemCount(givenDto).block());
    }

    @Test
    void getItemsCartWithTotal_shouldReturnCartDtoWithTotalCountSuccess() {
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID));
        Item expectedItem = new Item(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L);
        AccountBalanceDto expectedBalanceDto = new AccountBalanceDto(VALID_ID, 3000L, true, true);
        CartDto expectedDto = new CartDto(List.of(itemDto), 2000L, expectedBalanceDto);
        AccountResponse expectedAccountResponse = new AccountResponse();
        expectedAccountResponse.setAccountId(VALID_ID);
        expectedAccountResponse.setBalance(3000L);
        when(itemRepository.findAllById(Set.of(VALID_ID))).thenReturn(Flux.fromIterable(List.of(expectedItem)));
        when(paymentApi.getAccountById(VALID_ID)).thenReturn(Mono.just(expectedAccountResponse));

        CartDto actualDto = cartService.getItemsCartWithTotal(cartItemsCount).block();

        assertThat(actualDto).isNotNull().isEqualTo(expectedDto);
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
        public DefaultApi paymentApi() {
            return Mockito.mock(DefaultApi.class);
        }

        @Bean
        public CartService cartService() {
            return new CartServiceImpl(itemRepository(), itemMapper(), paymentApi());
        }
    }
}