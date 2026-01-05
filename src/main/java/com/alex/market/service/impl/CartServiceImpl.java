package com.alex.market.service.impl;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.output.CartDto;
import com.alex.market.model.Item;
import com.alex.market.service.CartService;
import reactor.core.publisher.Mono;

import java.util.Map;

public class CartServiceImpl implements CartService {
    @Override
    public Mono<Integer> changeItemCount(CartChangeDto cartChangeDto) {
        return null;
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
