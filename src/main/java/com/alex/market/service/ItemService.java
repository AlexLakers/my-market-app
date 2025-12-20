package com.alex.market.service;

import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;

public interface ItemService {

    PageItemsDto getItemsPage(SearchDto searchDto);

}
