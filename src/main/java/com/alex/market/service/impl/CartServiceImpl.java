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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public Integer incrementItemCount(Long itemId, Map<Long, Integer> cartItemsCount) {
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
    public Integer changeItemCount(CartChangeDto cartChangeDto) {
        if (!itemRepository.existsById(cartChangeDto.itemId())) {
            throw new ItemNotFoundException(cartChangeDto.itemId());
        }
        return switch (cartChangeDto.action()) {
            case PLUS -> incrementItemCount(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
            case MINUS -> decrementItemCount(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
            case DELETE -> deleteItem(cartChangeDto.itemId(), cartChangeDto.cartItemsCount());
        };
    }

    public Integer deleteItem(Long itemId, Map<Long, Integer> cartItemsCount) {
        cartItemsCount.remove(itemId);
        return 0;
    }

    @Override
    public CartDto getItemsCartWithTotal(Map<Long, Integer> cartItemsCount) {
        List<Item> items = getItemsByCart(cartItemsCount);

        Long totalPrice = items.stream()
                .map(it -> it.getPrice() * cartItemsCount.getOrDefault(it.getId(), 0))
                .reduce(0L, Long::sum);

        List<ItemDto> itemsDtos = items.stream()
                .map(entity -> itemMapper.toDto(entity, cartItemsCount))
                .toList();

        return buildCartDto(itemsDtos, totalPrice);
    }

    @Override
    public Map<Item, Integer> getItemsCartWithCounts(Map<Long, Integer> cartItemsCount) {
        return getItemsByCart(cartItemsCount).stream()
                .collect(Collectors.toMap(
                        item -> item,
                        item -> cartItemsCount.getOrDefault(item.getId(), 0)
                ));
    }

    private List<Item> getItemsByCart(Map<Long, Integer> cartItemsCount) {
        return itemRepository.findAllById(cartItemsCount.keySet());
    }

    private CartDto buildCartDto(List<ItemDto> items, Long totalPrice) {
        return new CartDto(items, totalPrice);
    }
}
