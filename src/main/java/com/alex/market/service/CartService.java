package com.alex.market.service;

import com.alex.market.api.dto.input.CartChangeDto;
import com.alex.market.api.dto.output.CartDto;
import com.alex.market.model.Item;

import java.util.Map;

public interface CartService {
    Integer changeItemCount(CartChangeDto cartChangeDto);
    CartDto getItemsCartWithTotal(Map<Long,Integer> cartItemsCount);
    Map<Item,Integer> getItemsCartWithCounts(Map<Long,Integer> cartItemsCount);

}
