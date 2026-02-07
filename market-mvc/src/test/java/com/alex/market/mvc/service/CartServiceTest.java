package com.alex.market.mvc.service;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.output.AccountBalanceDto;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.PaymentApiStatus;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;
    private static final Long GENERAL_ACCOUNT_ID = 1L;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private PaymentApiClientService paymentApiClientService;

    @Mock
    private ItemCacheService itemCacheService;

    @InjectMocks
    private CartServiceImpl cartService;

    private Map<Long, Integer> cartItemsCount;
    private ItemCache testItemCache;
    private Item testItem;
    private ItemDto testItemDto;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);

        testItemCache = new ItemCache(
                VALID_ID,
                "Test Item",
                "Test Description",
                1000L,
                "testImagePath.jpg"
        );

        testItem = Item.builder()
                .id(VALID_ID)
                .title("Test Item")
                .description("Test Description")
                .price(1000L)
                .imgPath("testImagePath.jpg")
                .build();

        testItemDto = new ItemDto(
                VALID_ID,
                "Test Item",
                "Test Description",
                "testImagePath.jpg",
                1000L,
                2
        );
    }

    @ParameterizedTest
    @CsvSource({"PLUS, 3", "MINUS, 1"})
    void changeItemCount_shouldReturnIncrementCountItemInCartSuccess(CartAction action, Integer expectedCount) {
        // Arrange
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, action, cartItemsCount);
        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));

        // Act
        Integer actualCount = cartService.changeItemCount(givenDto).block();

        // Assert
        assertThat(actualCount).isNotNull().isEqualTo(expectedCount);
        verify(itemRepository).existsById(VALID_ID);
    }

    @Test
    void changeItemCount_shouldThrowItemNotFoundException_whenItemIdNotExistsFail() {
        // Arrange
        CartChangeDto givenDto = new CartChangeDto(INVALID_ID, CartAction.PLUS, cartItemsCount);
        when(itemRepository.existsById(INVALID_ID)).thenReturn(Mono.just(false));

        // Act & Assert
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> cartService.changeItemCount(givenDto).block());

        verify(itemRepository).existsById(INVALID_ID);
    }

    @Test
    void changeItemCount_shouldReturnZeroWhenDeleteAction() {
        // Arrange
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.DELETE, cartItemsCount);
        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));

        // Act
        Integer actualCount = cartService.changeItemCount(givenDto).block();

        // Assert
        assertThat(actualCount).isEqualTo(0);
        assertThat(cartItemsCount).doesNotContainKey(VALID_ID);
    }

    @ParameterizedTest
    @CsvSource({
            "NETWORK_ERROR, ERROR",
            "SERVICE_ERROR, ERROR",
            "SUCCESS, ENOUGH",
            "SUCCESS, NOT_ENOUGH"
    })
    void getItemsCartWithBalanceStatus_shouldReturnCartDtoWithBalanceStatus(String sourceStatus, String targetStatus) {
        // Arrange
        AccountBalanceDto expectedBalanceDto = new AccountBalanceDto(
                GENERAL_ACCOUNT_ID,
                targetStatus.equals("ENOUGH") ? 3000L : 1000L,
                PaymentApiStatus.valueOf(sourceStatus)
        );

        CartDto expectedDto = new CartDto(
                List.of(testItemDto),
                2000L,
                targetStatus
        );

        // Мокаем ItemCacheService
        when(itemCacheService.getItemsWithCache(Set.of(VALID_ID)))
                .thenReturn(Flux.just(testItemCache));

        // Мокаем ItemMapper
        when(itemMapper.toItem(testItemCache)).thenReturn(testItem);
        when(itemMapper.toDto(testItem, cartItemsCount)).thenReturn(testItemDto);

        // Мокаем PaymentApiClientService
        when(paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID))
                .thenReturn(Mono.just(expectedBalanceDto));

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithBalanceStatus(cartItemsCount))
                .expectNextMatches(cartDto -> {
                    assertThat(cartDto.items()).hasSize(1);
                    assertThat(cartDto.total()).isEqualTo(2000L);
                    assertThat(cartDto.accountBalanceStatus()).isEqualTo(targetStatus);
                    return true;
                })
                .verifyComplete();

        // Verify mocks
        verify(itemCacheService).getItemsWithCache(Set.of(VALID_ID));
        verify(itemMapper).toItem(testItemCache);
        verify(itemMapper).toDto(testItem, cartItemsCount);
        verify(paymentApiClientService).getAccountById(GENERAL_ACCOUNT_ID);
    }

    @Test
    void getItemsCartWithBalanceStatus_shouldReturnEmptyCartWhenCartIsEmpty() {
        // Arrange
        Map<Long, Integer> emptyCart = new HashMap<>();

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithBalanceStatus(emptyCart))
                .expectNextMatches(cartDto -> {
                    assertThat(cartDto.items()).isEmpty();
                    assertThat(cartDto.total()).isEqualTo(0L);
                    assertThat(cartDto.accountBalanceStatus()).isEmpty();
                    return true;
                })
                .verifyComplete();

        // Verify no interactions with mocks when cart is empty
        verifyNoInteractions(itemCacheService, paymentApiClientService, itemMapper);
    }

    @Test
    void getItemsCartWithBalanceStatus_shouldReturnUnknownStatusForUnexpectedPaymentStatus() {
        // Arrange
        AccountBalanceDto unexpectedStatusBalance = new AccountBalanceDto(
                GENERAL_ACCOUNT_ID,
                3000L,
                PaymentApiStatus.BUSINESS_FAIL
        );

        when(itemCacheService.getItemsWithCache(Set.of(VALID_ID)))
                .thenReturn(Flux.just(testItemCache));
        when(itemMapper.toItem(testItemCache)).thenReturn(testItem);
        when(itemMapper.toDto(testItem, cartItemsCount)).thenReturn(testItemDto);
        when(paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID))
                .thenReturn(Mono.just(unexpectedStatusBalance));

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithBalanceStatus(cartItemsCount))
                .expectNextMatches(cartDto -> {
                    assertThat(cartDto.accountBalanceStatus()).isEqualTo("UNKNOWN");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getItemsCartWithBalanceStatus_shouldHandleEmptyResponseFromItemCache() {
        // Arrange
        AccountBalanceDto accountBalance = new AccountBalanceDto(
                GENERAL_ACCOUNT_ID,
                3000L,
                PaymentApiStatus.SUCCESS
        );

        when(itemCacheService.getItemsWithCache(Set.of(VALID_ID)))
                .thenReturn(Flux.empty());
        when(paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID))
                .thenReturn(Mono.just(accountBalance));

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithBalanceStatus(cartItemsCount))
                .expectNextMatches(cartDto -> {
                    assertThat(cartDto.items()).isEmpty();
                    assertThat(cartDto.total()).isEqualTo(0L);
                    assertThat(cartDto.accountBalanceStatus()).isEqualTo("ENOUGH");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getItemsCartWithBalanceStatus_shouldHandlePaymentServiceError() {
        // Arrange
        when(itemCacheService.getItemsWithCache(Set.of(VALID_ID)))
                .thenReturn(Flux.just(testItemCache));
        when(itemMapper.toItem(testItemCache)).thenReturn(testItem);
        // НЕ мокаем toDto, так как он не должен быть вызван при ошибке
        when(paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID))
                .thenReturn(Mono.error(new RuntimeException("Payment service error")));

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithBalanceStatus(cartItemsCount))
                .expectError(RuntimeException.class)
                .verify();

        // Verify that only expected mocks were called
        verify(itemCacheService).getItemsWithCache(Set.of(VALID_ID));
        verify(itemMapper).toItem(testItemCache);
        // НЕ проверяем toDto, так как он не должен вызываться при ошибке
        verify(itemMapper, never()).toDto(any(), any()); // Явно проверяем, что метод НЕ вызывался
        verify(paymentApiClientService).getAccountById(GENERAL_ACCOUNT_ID);
    }

    @Test
    void getItemsCartWithCounts_shouldReturnMapOfItemsWithCounts() {
        // Arrange
        when(itemCacheService.getItemsWithCache(Set.of(VALID_ID)))
                .thenReturn(Flux.just(testItemCache));
        when(itemMapper.toItem(testItemCache)).thenReturn(testItem);

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithCounts(cartItemsCount))
                .expectNextMatches(itemsMap -> {
                    assertThat(itemsMap).hasSize(1);
                    assertThat(itemsMap).containsKey(testItem);
                    assertThat(itemsMap.get(testItem)).isEqualTo(2);
                    return true;
                })
                .verifyComplete();

        verify(itemCacheService).getItemsWithCache(Set.of(VALID_ID));
        verify(itemMapper).toItem(testItemCache);
    }

    @Test
    void getItemsCartWithCounts_shouldReturnEmptyMapWhenCartIsEmpty() {
        // Arrange
        Map<Long, Integer> emptyCart = new HashMap<>();
        when(itemCacheService.getItemsWithCache(Collections.emptySet()))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithCounts(emptyCart))
                .expectNextMatches(itemsMap -> itemsMap.isEmpty())
                .verifyComplete();

        verify(itemCacheService).getItemsWithCache(Collections.emptySet());
        verifyNoInteractions(itemMapper);
    }

    @Test
    void getItemsCartWithCounts_shouldSkipItemsThatFailToLoad() {
        // Arrange
        ItemCache itemCache2 = new ItemCache(2L, "Item 2", "Desc 2", 500L, "img2.jpg");
        Item item2 = Item.builder().id(2L).title("Item 2").description("Desc 2").price(500L).imgPath("img2.jpg").build();

        Map<Long, Integer> cart = new HashMap<>();
        cart.put(VALID_ID, 2);
        cart.put(2L, 1);
        cart.put(3L, 1); // This one will fail to load

        when(itemCacheService.getItemsWithCache(Set.of(VALID_ID, 2L, 3L)))
                .thenReturn(Flux.just(testItemCache, itemCache2).concatWith(Flux.error(new RuntimeException("Failed to load item 3"))));
        when(itemMapper.toItem(testItemCache)).thenReturn(testItem);
        when(itemMapper.toItem(itemCache2)).thenReturn(item2);

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithCounts(cart))
                .expectNextMatches(itemsMap -> {
                    // Should only contain the two successfully loaded items
                    assertThat(itemsMap).hasSize(2);
                    assertThat(itemsMap).containsKeys(testItem, item2);
                    assertThat(itemsMap.get(testItem)).isEqualTo(2);
                    assertThat(itemsMap.get(item2)).isEqualTo(1);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void incrementItemCount_shouldWorkCorrectly() {
        // Arrange
        Map<Long, Integer> cart = new HashMap<>();
        cart.put(VALID_ID, 1);
        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));

        // Act
        Integer newCount = cartService.changeItemCount(new CartChangeDto(VALID_ID, CartAction.PLUS, cart)).block();

        // Assert
        assertThat(newCount).isEqualTo(2);
        assertThat(cart.get(VALID_ID)).isEqualTo(2);
        verify(itemRepository).existsById(VALID_ID);
    }

    @Test
    void decrementItemCount_shouldRemoveItemWhenCountReachesZero() {
        // Arrange
        Map<Long, Integer> cart = new HashMap<>();
        cart.put(VALID_ID, 1);

        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));

        // Act
        Integer newCount = cartService.changeItemCount(new CartChangeDto(VALID_ID, CartAction.MINUS, cart)).block();

        // Assert
        assertThat(newCount).isEqualTo(0);
        assertThat(cart).doesNotContainKey(VALID_ID);
    }

    @Test
    void decrementItemCount_shouldNotGoBelowZero() {
        // Arrange
        Map<Long, Integer> cart = new HashMap<>();
        cart.put(VALID_ID, 1);

        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));

        // First decrement to 0
        cartService.changeItemCount(new CartChangeDto(VALID_ID, CartAction.MINUS, cart)).block();

        // Try to decrement again (should not affect the cart since item is removed)
        Integer newCount = cartService.changeItemCount(new CartChangeDto(VALID_ID, CartAction.MINUS, cart)).block();

        // Assert
        assertThat(newCount).isEqualTo(0);
        assertThat(cart).doesNotContainKey(VALID_ID);
    }

    @Test
    void calculateCartDto_shouldCalculateCorrectTotal() {
        // Arrange
        ItemCache itemCache2 = new ItemCache(2L, "Item 2", "Desc 2", 500L, null);
        Item item2 = Item.builder().id(2L).title("Item 2").description("Desc 2").price(500L).build();
        ItemDto itemDto2 = new ItemDto(2L, "Item 2", "Desc 2", "", 500L, 1);

        Map<Long, Integer> cart = new HashMap<>();
        cart.put(VALID_ID, 2);
        cart.put(2L, 1);

        AccountBalanceDto accountBalance = new AccountBalanceDto(
                GENERAL_ACCOUNT_ID,
                5000L,
                PaymentApiStatus.SUCCESS
        );

        when(itemCacheService.getItemsWithCache(Set.of(VALID_ID, 2L)))
                .thenReturn(Flux.just(testItemCache, itemCache2));
        when(itemMapper.toItem(testItemCache)).thenReturn(testItem);
        when(itemMapper.toItem(itemCache2)).thenReturn(item2);
        when(itemMapper.toDto(testItem, cart)).thenReturn(testItemDto);
        when(itemMapper.toDto(item2, cart)).thenReturn(itemDto2);
        when(paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID))
                .thenReturn(Mono.just(accountBalance));

        // Act & Assert
        StepVerifier.create(cartService.getItemsCartWithBalanceStatus(cart))
                .expectNextMatches(cartDto -> {
                    assertThat(cartDto.items()).hasSize(2);
                    assertThat(cartDto.accountBalanceStatus()).isEqualTo("ENOUGH"); // 5000 >= 2500
                    return true;
                })
                .verifyComplete();
    }
}