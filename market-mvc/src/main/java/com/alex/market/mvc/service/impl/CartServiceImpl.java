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
import com.alex.market.mvc.service.ItemCacheService;
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
    private final ItemCacheService itemCacheService;

    private final static Long GENERAL_ACCOUNT_ID = 1L;

    @Override
    public Mono<Integer> changeItemCount(CartChangeDto cartChangeDto) {
        log.debug("Change item count: id={}, action={}",
                cartChangeDto.itemId(), cartChangeDto.action());

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
                        log.debug("New count for item: id={}, count={}", cartChangeDto.itemId(), newCount)
                );
    }

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

    private Integer deleteItem(Long itemId, Map<Long, Integer> cartItemsCount) {
        cartItemsCount.remove(itemId);
        return 0;
    }

    @Override
    public Mono<CartDto> getItemsCartWithBalanceStatus(Map<Long, Integer> cartItemsCount) {
        log.info("Getting cart with balance status, items count: {}",
                cartItemsCount != null ? cartItemsCount.size() : 0);

        if (cartItemsCount == null || cartItemsCount.isEmpty()) {
            return Mono.just(new CartDto(List.of(), 0L, ""));
        }

        Mono<AccountBalanceDto> accountBalance =
                paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID);

        return loadItemsWithCache(cartItemsCount.keySet())
                .collectList()
                .zipWith(accountBalance)
                .flatMap(tuple -> {
                    List<Item> items = tuple.getT1();
                    AccountBalanceDto accDto = tuple.getT2();
                    return calculateCartDto(items, cartItemsCount, accDto);
                });
    }

    @Override
    public Mono<Map<Item, Integer>> getItemsCartWithCounts(Map<Long, Integer> cartItemsCount) {
        return loadItemsWithCache(cartItemsCount.keySet())
                .collectMap(
                        item -> item,
                        item -> cartItemsCount.getOrDefault(item.getId(), 0)
                );
    }

    private Flux<Item> loadItemsWithCache(Set<Long> ids) {
        return itemCacheService.getItemsWithCache(ids)
                .map(itemMapper::toItem)
                .onErrorResume(e -> {
                    log.warn("Skipping item due to error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<CartDto> calculateCartDto(List<Item> items,
                                           Map<Long, Integer> cartItemsCount,
                                           AccountBalanceDto accountBalanceDto) {
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
            case SUCCESS -> isEnoughMoney(accountBalanceDto.balance(), amount) ?
                    "ENOUGH" : "NOT_ENOUGH";
            case SERVICE_ERROR, NETWORK_ERROR -> "ERROR";
            default -> "UNKNOWN";
        };

        return Mono.just(new CartDto(itemDtos, amount, accountBalanceStatus));
    }

    private boolean isEnoughMoney(Long balance, Long amount) {
        return balance.compareTo(amount) >= 0;
    }
}


