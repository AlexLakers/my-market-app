package com.alex.market.api.dto.output;

import java.util.List;

public record CartDto(List<ItemDto> items,Long total) {
}
