package com.alex.market.service.impl;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.output.CartDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;


    private Integer incrementItemCount(Long itemId, Map<Long, Integer> cartItemsCount) {
        return cartItemsCount.merge(itemId, 1, Integer::sum);
    }

    private Integer decrementItemCount(Long itemId, Map<Long, Integer> cartItemsCount) {
        Integer result = cartItemsCount.computeIfPresent(itemId, (id, currentQty) -> {
            int newQty = currentQty - 1;
            return newQty > 0 ? newQty : null;
        });
        return result == null ? 0 : result;
    }

    @Override
    public Mono<Integer> changeItemCount(CartChangeDto cartChangeDto) {
        return itemRepository.existsById(cartChangeDto.itemId())
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(cartChangeDto.itemId())))
                .then(Mono.just(
                        switch (cartChangeDto.action()) {
                            case PLUS -> incrementItemCount(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
                            case MINUS -> decrementItemCount(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
                            case DELETE -> deleteItem(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
                        }));
    }

    public Integer deleteItem(Long itemId, Map<Long, Integer> cartItemsCount) {
        cartItemsCount.remove(itemId);
        return 0;
    }

    @Override
    public Mono<CartDto> getItemsCartWithTotal(Map<Long, Integer> cartItemsCount) {
        return null;
    }

    @Override
    public Mono<Map<Item, Integer>> getItemsCartWithCounts(Map<Long, Integer> cartItemsCount) {
        return null;
    }

}
