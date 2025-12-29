package com.alex.market.dto.output;

import java.util.List;

public record CartDto(List<ItemDto> items,Long total) {
}
