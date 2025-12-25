package com.alex.market.service;

import com.alex.market.api.dto.input.CartChangeDto;
import com.alex.market.api.dto.input.ItemCreateDto;
import com.alex.market.api.dto.output.ItemDto;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;

import java.util.Map;

public interface ItemService {

    PageItemsDto getItemsPage(SearchDto searchDto);

    ItemDto findByIdWithCartCount(Long id, Map<Long,Integer> cartCountMap);

    ItemDto changeCartItemCount(CartChangeDto cartChangeDto);

    ItemDto createItem(ItemCreateDto itemCreateDto);
}
