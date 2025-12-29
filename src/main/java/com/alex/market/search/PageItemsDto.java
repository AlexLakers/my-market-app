package com.alex.market.search;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;

import java.util.List;

public record PageItemsDto(List<List<ItemDto>> items, String search, String sort, PageDto pageDto) {
}
