package com.alex.market.mvc.service;

import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.input.ItemCreateDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.search.PageItemsDto;
import com.alex.market.mvc.search.SearchDto;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ItemService {
    Mono<PageItemsDto> getItemsPage(SearchDto searchDto);

    Mono<ItemDto> createItem(ItemCreateDto itemCreateDto);

    Mono<ItemDto> getItemByIdWithCartCount(Long id, Map<Long, Integer> cartCountMap);

   Mono <ItemDto> changeCartItemCount(CartChangeDto cartChangeDto);
}
