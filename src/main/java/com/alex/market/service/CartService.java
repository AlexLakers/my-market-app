package com.alex.market.service;

import com.alex.market.api.dto.CartChangeDto;
import com.alex.market.api.dto.CartDto;

import java.util.Map;

public interface CartService {
    Integer changeItemCount(CartChangeDto cartChangeDto);
    CartDto getCartItems(Map<Long,Integer> cartItemsCount);

}
