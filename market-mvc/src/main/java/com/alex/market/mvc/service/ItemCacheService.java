package com.alex.market.mvc.service;

import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.search.PageItemsDto;
import com.alex.market.mvc.search.SearchDto;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ItemCacheService {
    Mono<ItemDto> getItemById(Long id, Map<Long, Integer> map);
    Mono<PageItemsDto> getItemsPage(SearchDto searchDto);
}
