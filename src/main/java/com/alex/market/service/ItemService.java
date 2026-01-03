package com.alex.market.service;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import reactor.core.publisher.Mono;

public interface ItemService {
    Mono<PageItemsDto> getItemsPage(SearchDto searchDto);

    Mono<ItemDto> createItem(ItemCreateDto itemCreateDto);
}
