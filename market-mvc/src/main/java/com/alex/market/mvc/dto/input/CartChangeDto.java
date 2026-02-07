package com.alex.market.mvc.dto.input;

import com.alex.market.mvc.model.CartAction;

import java.util.Map;

public record CartChangeDto(Long itemId,
                            CartAction action,
                            Map<Long, Integer> cartItemsCount) {
}
