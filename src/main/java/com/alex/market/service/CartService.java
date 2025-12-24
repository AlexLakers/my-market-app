package com.alex.market.service;

import com.alex.market.api.dto.input.CartChangeDto;
import com.alex.market.api.dto.output.CartDto;

import java.util.Map;

public interface CartService {
    Integer changeItemCount(CartChangeDto cartChangeDto);
    CartDto getItems(Map<Long,Integer> cartItemsCount);

}
