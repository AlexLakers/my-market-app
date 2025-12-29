package com.alex.market.service;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ItemService {

    PageItemsDto getItemsPage(SearchDto searchDto);

    ItemDto findByIdWithCartCount(Long id, Map<Long,Integer> cartCountMap);

    ItemDto changeCartItemCount(CartChangeDto cartChangeDto);

    ItemDto createItem(ItemCreateDto itemCreateDto);
}
