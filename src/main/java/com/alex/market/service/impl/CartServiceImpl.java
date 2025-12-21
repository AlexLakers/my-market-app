package com.alex.market.service.impl;

import com.alex.market.api.dto.CartChangeDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final ItemRepository itemRepository;


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
        };
    }
}
