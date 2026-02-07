package com.alex.market.mvc.service;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import com.alex.market.mvc.search.SearchDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface ItemCacheService {
    Mono<ItemCache> getItemFromCache(Long id);

    Mono<Void> saveItemToCache(ItemCache itemCache);

    Mono<ItemCache> getItemWithCache(Long id);

    Flux<ItemCache> getItemsWithCache(Set<Long> ids);

    Mono<String> getImageFromCache(String imgPath);

    Mono<Void> saveImageToCache(String imgPath, String imageUri);

    Mono<PageInfoCache> getPageFromCache(SearchDto searchDto);

    Mono<Void> savePageToCache(SearchDto searchDto, PageInfoCache pageInfoCache);

    Mono<PageInfoCache> getPageWithCache(SearchDto searchDto, Pageable pageable);

}
