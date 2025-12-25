package com.alex.market.api.dto.output;

import java.util.List;

public record OrderDto(Long id, List<ItemDto> items, Long totalSum) {
}
