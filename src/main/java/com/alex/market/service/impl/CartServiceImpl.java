package com.alex.market.service.impl;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.output.CartDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;


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

    public Integer deleteItem(Long itemId, Map<Long, Integer> cartItemsCount) {
        cartItemsCount.remove(itemId);
        return 0;
    }

    @Override
    public Mono<CartDto> getItemsCartWithTotal(Map<Long, Integer> cartItemsCount) {
        log.info("Getting cart with total, items count: {}", cartItemsCount != null ? cartItemsCount.size() : 0);

        if (cartItemsCount == null || cartItemsCount.isEmpty()) {
            log.debug("Empty cart, returning empty DTO");
            return Mono.just(new CartDto(List.of(), 0L));
        }

        return getItemsByCart(cartItemsCount)
                .collectList()
                .flatMap(items -> buildCartDto(items, cartItemsCount))
                .doOnSuccess(cart -> log.debug("Cart loaded with {} items, total: {}",
                        cart.items().size(), cart.total()))
                .doOnError(error -> log.error("Failed to load cart", error));
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
        return itemRepository.findAllById(cartItemsCount.keySet());
    }

    private Mono<CartDto> buildCartDto(List<Item> items, Map<Long, Integer> cartItemsCount) {
        log.trace("Building cart from {} items", items.size());
        return Flux.fromIterable(items)
                .map(item -> {
                    Integer count = cartItemsCount.getOrDefault(item.getId(), 0);
                    ItemDto dto = itemMapper.toDto(item, cartItemsCount);
                    long itemTotal = item.getPrice() * count;
                    return Tuples.of(dto, itemTotal);
                })
                .collectList()
                .map(list -> {
                    List<ItemDto> itemDtos = list.stream()
                            .map(Tuple2::getT1)
                            .collect(Collectors.toList());
                    Long totalPrice = list.stream()
                            .mapToLong(Tuple2::getT2)
                            .sum();
                    log.debug("Cart built: {} items, total {}", itemDtos.size(), totalPrice);
                    return new CartDto(itemDtos, totalPrice);
                })
                .doOnError(error ->
                        log.error("Failed to build cart", error)
                );
    }
}


