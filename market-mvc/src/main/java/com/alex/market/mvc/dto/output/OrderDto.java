package com.alex.market.mvc.dto.output;

import java.util.List;

public record OrderDto(Long id, List<ItemDto> items, Long totalSum) {
}
