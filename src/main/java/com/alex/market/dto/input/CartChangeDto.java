package com.alex.market.dto.input;

import com.alex.market.model.CartAction;

import java.util.Map;

public record CartChangeDto(Long itemId,
                            CartAction action,
                            Map<Long, Integer> cartItemsCount) {
}
