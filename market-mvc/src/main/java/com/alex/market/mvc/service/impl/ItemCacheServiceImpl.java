package com.alex.market.mvc.service.impl;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.search.SearchDto;
import com.alex.market.mvc.service.ItemCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemCacheServiceImpl implements ItemCacheService {

    private final static String ITEM_DATA_PREFIX = "item:data:%d";
    private final static String ITEM_IMAGE_PREFIX = "item:image:%s";
    private final static String ITEMS_PAGE_SET_PREFIX = "items:page:search:%1$s:sort:%2$s:page:%3$d:size:%4$s";
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    private final ReactiveRedisTemplate<String, ItemCache> itemCacheTemplate;
    private final ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheTemplate;
    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public Mono<ItemCache> getItemFromCache(Long id) {
        String key = buildItemDataKey(id);
        return itemCacheTemplate.opsForValue().get(key);
    }

    public Mono<Void> saveItemToCache(ItemCache itemCache) {
        String key = buildItemDataKey(itemCache.id());
        return itemCacheTemplate.opsForValue()
                .set(key, itemCache, CACHE_TTL)
                .then();
    }

    public Mono<ItemCache> getItemWithCache(Long id) {
        return getItemFromCache(id)
                .switchIfEmpty(Mono.defer(() ->
                        loadItemFromDb(id)
                                .flatMap(itemCache ->
                                        saveItemToCache(itemCache)
                                                .thenReturn(itemCache)
                                )
                ));
    }

    public Flux<ItemCache> getItemsWithCache(Set<Long> ids) {
        return Flux.fromIterable(ids)
                .flatMap(this::getItemWithCache)
                .filter(itemCache -> itemCache != null);
    }

    private Mono<ItemCache> loadItemFromDb(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .map(itemMapper::toCache)
                .onErrorResume(ItemNotFoundException.class, e -> {
                    log.warn("Item not found in DB: {}", id);
                    return Mono.empty();
                });
    }

    public Mono<String> getImageFromCache(String imgPath) {
        if (imgPath == null || imgPath.isEmpty()) {
            return Mono.empty();
        }
        String key = buildItemImageKey(imgPath);
        return stringRedisTemplate.opsForValue().get(key);
    }

    public Mono<Void> saveImageToCache(String imgPath, String imageUri) {
        if (imgPath == null || imgPath.isEmpty()) {
            return Mono.empty();
        }
        String key = buildItemImageKey(imgPath);
        return stringRedisTemplate.opsForValue()
                .set(key, imageUri, CACHE_TTL)
                .then();
    }

    public Mono<PageInfoCache> getPageFromCache(SearchDto searchDto) {
        String key = buildPageKey(searchDto);
        return pageInfoCacheTemplate.opsForValue().get(key);
    }

    public Mono<Void> savePageToCache(SearchDto searchDto, PageInfoCache pageInfoCache) {
        String key = buildPageKey(searchDto);
        return pageInfoCacheTemplate.opsForValue()
                .set(key, pageInfoCache, CACHE_TTL)
                .then();
    }

    public Mono<PageInfoCache> getPageWithCache(SearchDto searchDto, Pageable pageable) {
        return getPageFromCache(searchDto)
                .switchIfEmpty(Mono.defer(() ->
                        loadPageFromDb(searchDto, pageable)
                                .flatMap(pageInfo ->
                                        savePageToCache(searchDto, pageInfo)
                                                .thenReturn(pageInfo)
                                )
                ));
    }

    private Mono<PageInfoCache> loadPageFromDb(SearchDto searchDto, Pageable pageable) {
        return itemRepository.findAll(searchDto.search(), pageable)
                .map(page -> {
                    List<Long> itemIds = page.getContent().stream()
                            .map(Item::getId)
                            .collect(Collectors.toList());

                    return new PageInfoCache(
                            itemIds,
                            page.getSize(),
                            page.getNumber() + 1,
                            page.hasPrevious(),
                            page.hasNext()
                    );
                });
    }

    private String buildItemDataKey(Long id) {
        return ITEM_DATA_PREFIX.formatted(id);
    }

    private String buildItemImageKey(String imgPath) {
        return ITEM_IMAGE_PREFIX.formatted(imgPath);
    }

    private String buildPageKey(SearchDto searchDto) {
        String searchPart = searchDto.search() != null ? searchDto.search() : "";
        return ITEMS_PAGE_SET_PREFIX.formatted(
                searchPart,
                searchDto.sortColumn().name(),
                searchDto.pageNumber(),
                searchDto.pageSize()
        );
    }
}
