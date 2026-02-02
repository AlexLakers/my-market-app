package com.alex.market.mvc.service.impl;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.output.AccountBalanceDto;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.service.CartService;
import com.alex.market.mvc.service.ItemService;
import com.alex.market.mvc.service.PaymentApiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final PaymentApiClientService paymentApiClientService;
    private final ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate;

    private final static Long GENERAL_ACCOUNT_ID = 1L;
    private final static String ITEM_DATA_PREFIX = "item:data:%d";
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    private Integer incrementItemCount(Long itemId, Map<Long, Integer> cartItemsCount) {
        return cartItemsCount.merge(itemId, 1, Integer::sum);
    }

    private Integer decrementItemCount(Long itemId, Map<Long, Integer> cartItemsCount) {
        Integer result = cartItemsCount.computeIfPresent(itemId, (id, currentCount) -> {
            int newCount = currentCount - 1;
            return newCount > 0 ? newCount : null;
        });
        return result == null ? 0 : result;
    }

    @Override
    public Mono<Integer> changeItemCount(CartChangeDto cartChangeDto) {
        log.debug("Change item count with id: {}, action: {}", cartChangeDto.itemId(), cartChangeDto.action());

        return itemRepository.existsById(cartChangeDto.itemId())
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(cartChangeDto.itemId())))
                .then(Mono.just(
                        switch (cartChangeDto.action()) {
                            case PLUS -> incrementItemCount(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
                            case MINUS -> decrementItemCount(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
                            case DELETE -> deleteItem(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
                        }))
                .doOnNext(newCount ->
                        log.debug("New count for item with id: {} {}", cartChangeDto.itemId(), newCount))
                .doOnError(ItemNotFoundException.class, error ->
                        log.warn("Cannot change count: item wit id: {} not found", cartChangeDto.itemId()))
                .doOnError(error ->
                        log.error("Failed to change item count", error));
    }

    private Integer deleteItem(Long itemId, Map<Long, Integer> cartItemsCount) {
        cartItemsCount.remove(itemId);
        return 0;
    }

    @Override
    public Mono<CartDto> getItemsCartWithBalanceStatus(Map<Long, Integer> cartItemsCount) {
        log.info("Getting cart with total, items count: {}", cartItemsCount != null ? cartItemsCount.size() : 0);

        if (cartItemsCount == null || cartItemsCount.isEmpty()) {
            log.debug("Empty cart, returning empty DTO");
            return Mono.just(new CartDto(List.of(), 0L, ""));
        }

        Mono<AccountBalanceDto> accountBalanceDtoMono = paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID);


        Mono<List<Item>> itemsMono = getItemsByCart(cartItemsCount).collectList();


        return itemsMono.zipWith(accountBalanceDtoMono)
                .flatMap(tuple -> {
                    List<Item> items = tuple.getT1();
                    AccountBalanceDto accDto = tuple.getT2();

                    return calculateCartDto(items, cartItemsCount, accDto);
                })
                .doOnSuccess(cartDto ->
                        log.info("Successfully calculated cart: {}", cartDto)
                )
                .doOnError(error -> log.error("Failed to calculate cart", error));
    }


    private Mono<CartDto> calculateCartDto(List<Item> items, Map<Long, Integer> cartItemsCount, AccountBalanceDto accountBalanceDto) {


        List<ItemDto> itemDtos = items.stream()
                .map(item -> itemMapper.toDto(item, cartItemsCount))
                .collect(Collectors.toList());

        Long amount = itemDtos.stream()
                .mapToLong(dto -> {
                    Integer count = cartItemsCount.getOrDefault(dto.id(), 0);
                    return count * dto.price();
                })
                .sum();

        String accountBalanceStatus = switch (accountBalanceDto.status()) {

            case SUCCESS -> isEnoughMoney(accountBalanceDto.balance(), amount) ? "ENOUGH" : "NOT_ENOUGH";

            case SERVICE_ERROR, NETWORK_ERROR -> "ERROR";

            default -> "UNKNOWN";

        };


        return Mono.just(new CartDto(itemDtos, amount, accountBalanceStatus));
    }

    private boolean isEnoughMoney(Long balance, Long amount) {
        return balance.compareTo(amount) >= 0;
    }


    @Override
    public Mono<Map<Item, Integer>> getItemsCartWithCounts(Map<Long, Integer> cartItemsCount) {
        int cartSize = cartItemsCount != null ? cartItemsCount.size() : 0;
        log.debug("Getting items with counts, cart size: {}", cartSize);

        return getItemsByCart(cartItemsCount)
                .collectMap(item -> item, item -> cartItemsCount.getOrDefault(item.getId(), 0))
                .doOnNext(itemsMap ->
                        log.info("Cart items with counts retrieved: {} items", itemsMap.size())
                )
                .doOnError(error ->
                        log.error("Failed to get cart items with counts: {}", error.getMessage())
                );
    }

    private Flux<Item> getItemsByCart(Map<Long, Integer> cartItemsCount) {

        Set<Long> ids = cartItemsCount.keySet();

        return Flux.fromIterable(ids)
                .flatMap(id -> {
                    String itemKey = buildItemDataKey(id);
                    return itemCacheReactiveRedisTemplate.opsForValue().get(itemKey)
                            .switchIfEmpty(loadAndCacheItem(id))
                            .map(itemMapper::toItem)
                            .onErrorResume(e -> {
                                log.warn("Skipping item {} due to error: {}", id, e.getMessage());
                                return Mono.empty();
                            });

                }, 10);

    }

    private String buildItemDataKey(Long id) {
        return ITEM_DATA_PREFIX.formatted(id);
    }

    private Mono<ItemCache> loadAndCacheItem(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .map(item -> {
                    log.debug("Loading item with id: {} from database:", id);
                    return itemMapper.toCache(item);
                })
                .flatMap(itemCache -> {
                    String cacheKey = buildItemDataKey(id);

                    return itemCacheReactiveRedisTemplate.opsForValue().set(cacheKey, itemCache, CACHE_TTL)
                            .doOnNext(isSet -> {
                                if (Boolean.TRUE.equals(isSet)) {
                                    log.debug("Cached item with id: {}", id);
                                } else {
                                    log.warn("Failed to cache item with id: {}", id);
                                }
                            })
                            .onErrorResume(e -> {
                                log.error("Cache failed for item with id: {}: {}", id, e.getMessage());

                                return Mono.empty();
                            })
                            .thenReturn(itemCache);
                });
    }
}


