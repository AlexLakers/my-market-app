package com.alex.market.service;

import com.alex.market.api.dto.CartChangeDto;
import com.alex.market.api.dto.ItemDto;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;

import java.util.Map;
import java.util.Optional;

public interface ItemService {

    PageItemsDto getItemsPage(SearchDto searchDto);

    ItemDto findByIdWithCartCount(Long id, Map<Long,Integer> cartCountMap);

    Integer changeCartItemCount(CartChangeDto cartChangeDto);

}
