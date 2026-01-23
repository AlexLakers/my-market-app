package com.alex.market.mvc.service;

import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.model.Item;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface CartService {
    Mono<Integer> changeItemCount(CartChangeDto cartChangeDto);

    Mono<CartDto> getItemsCartWithBalanceStatus(Map<Long, Integer> cartItemsCount);

    Mono<Map<Item, Integer>> getItemsCartWithCounts(Map<Long, Integer> cartItemsCount);

}
