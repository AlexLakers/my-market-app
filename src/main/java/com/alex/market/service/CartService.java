package com.alex.market.service;

import com.alex.market.api.dto.CartChangeDto;

import java.util.Map;

public interface CartService {
    Integer changeItemCount(CartChangeDto cartChangeDto);
}
