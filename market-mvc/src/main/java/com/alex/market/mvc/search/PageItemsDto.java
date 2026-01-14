package com.alex.market.mvc.search;

import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.PageDto;

import java.util.List;

public record PageItemsDto(List<List<ItemDto>> items, String search, String sort, PageDto pageDto) {
}
