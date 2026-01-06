package com.alex.market.service;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.output.CartDto;
import com.alex.market.model.Item;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface CartService {
    Mono<Integer> changeItemCount(CartChangeDto cartChangeDto);

    Mono<CartDto> getItemsCartWithTotal(Map<Long, Integer> cartItemsCount);

    Mono<Map<Item, Integer>> getItemsCartWithCounts(Map<Long, Integer> cartItemsCount);

}
