package com.alex.market.api.dto;

import com.alex.market.model.Item;

import java.util.List;

public record CartDto(List<ItemDto> items,Long total) {
}
