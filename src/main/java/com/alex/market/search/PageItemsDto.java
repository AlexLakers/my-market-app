package com.alex.market.search;

import com.alex.market.api.dto.ItemDto;
import com.alex.market.api.dto.PageDto;

import java.util.List;

public record PageItemsDto(List<List<ItemDto>> items, String search, String sort, PageDto pageDto) {
}
