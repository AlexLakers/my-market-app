package com.alex.market.mvc.service.impl;


import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.input.ItemCreateDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.PageDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.exception.TitleAlreadyExistsException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.search.ItemSort;
import com.alex.market.mvc.search.PageItemsDto;
import com.alex.market.mvc.search.SearchDto;
import com.alex.market.mvc.service.CartService;
import com.alex.market.mvc.service.ImageService;
import com.alex.market.mvc.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final static Integer CONTENT_GROUP_SIZE = 3;
    private final static String ITEM_DATA_PREFIX = "item:data:%d";
    private final static String ITEM_IMAGE_PREFIX = "item:image:%s";
    private final static String ITEMS_PAGE_SET_PREFIX = "items:page:search:%1$s:sort:%2$s:page:%3$d:size:%4$s";
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CartService cartService;
    private final ImageService imageService;

    private final ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate;
    private final ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;


    @Override
    public Mono<PageItemsDto> getItemsPage(SearchDto searchDto) {
        log.info("Getting page with items: pageNumber={}, pageSize={}, sortColumn={}, search={}, cartItemsCount={}",
                searchDto.pageNumber(),
                searchDto.pageSize(),
                searchDto.sortColumn(),
                searchDto.search(),
                searchDto.cartItemsCount());


        String pageKey = generatePageKey(searchDto);


        Pageable pageable = PageRequest.of(
                searchDto.pageNumber() - 1,
                searchDto.pageSize(),
                ItemSort.getOrderByPriceOrTitle(searchDto.sortColumn()));


        return pageInfoCacheReactiveRedisTemplate.opsForValue().get(pageKey)
                .switchIfEmpty(Mono.defer(() -> {

                    return loadAndCachePage(searchDto, pageable, pageKey);
                }))
                .flatMap(pageInfoCache -> {

                    return loadAndCacheItems(pageInfoCache.itemsIds())
                            .collectList()
                            .map(itemCaches -> {

                                return toPageItemsDto(searchDto, itemCaches, pageInfoCache);
                            });
                })
                .doOnNext(result ->
                        log.info("Successful return page of items in {} groups", result.items().size())
                )
                .doOnError(error ->
                        log.error("Error during handling page: {}", error.getMessage(), error)
                );
    }

    private PageItemsDto toPageItemsDto(SearchDto searchDto, List<ItemCache> itemCaches, PageInfoCache pageInfoCache) {
        List<ItemCache> validCaches = itemCaches.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        List<ItemDto> itemDtos = validCaches.stream()
                .map(cache -> itemMapper.toDtoFromItemCacheWithoutImage(cache, searchDto.cartItemsCount()))
                .collect(Collectors.toList());


        List<List<ItemDto>> groupedItems = groupItems(itemDtos, CONTENT_GROUP_SIZE);

        return new PageItemsDto(
                groupedItems,
                searchDto.search(),
                searchDto.sortColumn().name(),
                new PageDto(searchDto.pageSize(), searchDto.pageNumber(), pageInfoCache.hasPrevious(), pageInfoCache.hasNext()));
    }

    private Flux<ItemCache> loadAndCacheItems(List<Long> itemIds) {
        return Flux.fromIterable(itemIds)
                .flatMap(id -> {
                    String itemKey = buildItemDataKey(id);

                    return itemCacheReactiveRedisTemplate.opsForValue()
                            .get(itemKey)
                            .switchIfEmpty(Mono.defer(() -> {

                                return itemRepository.findById(id)
                                        .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                                        .flatMap(item -> {
                                            ItemCache itemCache = itemMapper.toCache(item);
                                            return itemCacheReactiveRedisTemplate.opsForValue()
                                                    .set(itemKey, itemCache, CACHE_TTL)
                                                    .thenReturn(itemCache);
                                        });
                            }))
                            .onErrorResume(ItemNotFoundException.class, e -> {
                                log.warn("Item with id: {} not found in DB", id);
                                return Mono.empty();
                            })
                            .onErrorResume(e -> {
                                log.warn("Failed to get item with id: {} from cache/DB with message: {}", id, e.getMessage());
                                return Mono.empty();
                            });
                });
    }

    private Mono<PageInfoCache> loadAndCachePage(SearchDto searchDto, Pageable pageable, String pageKey) {
        return itemRepository.findAll(searchDto.search(), pageable)
                .flatMap(page -> {

                    PageInfoCache pageInfo = new PageInfoCache(
                            page.getContent().stream()
                                    .map(Item::getId)
                                    .collect(Collectors.toList()),
                            page.getSize(),
                            page.getNumber() + 1,
                            page.hasPrevious(),
                            page.hasNext()
                    );


                    List<Mono<Void>> allCacheOperations = new ArrayList<>();


                    page.getContent().forEach(item -> {
                        ItemCache itemCache = itemMapper.toCache(item);
                        String itemKey = buildItemDataKey(itemCache.id());

                        Mono<Void> itemCacheOp = itemCacheReactiveRedisTemplate.opsForValue()
                                .set(itemKey, itemCache, CACHE_TTL)
                                .doOnSuccess(isSet -> {
                                    if (Boolean.TRUE.equals(isSet)) {
                                        log.debug("Cached item with id: {}", itemCache.id());
                                    }
                                })
                                .then();

                        allCacheOperations.add(itemCacheOp);
                    });

                    Mono<Void> pageCacheOp = pageInfoCacheReactiveRedisTemplate.opsForValue()
                            .set(pageKey, pageInfo, CACHE_TTL)
                            .doOnSuccess(isSet -> {
                                if (Boolean.TRUE.equals(isSet)) {
                                    log.debug("Cached page using key:{}", pageKey);
                                }
                            })
                            .then();

                    allCacheOperations.add(pageCacheOp);

                    return Mono.when(allCacheOperations)
                            .thenReturn(pageInfo);
                });
    }

    private String generatePageKey(SearchDto searchDto) {
        String searchPart = searchDto.search() != null ? searchDto.search() : "";

        return ITEMS_PAGE_SET_PREFIX.formatted(searchPart, searchDto.sortColumn().name(), searchDto.pageNumber(), searchDto.pageSize());
    }

    @Override
    @Transactional
    public Mono<ItemDto> createItem(ItemCreateDto itemCreateDto) {
        log.info("Creating item with: title={}, price={}", itemCreateDto.title(), itemCreateDto.price());

        return itemRepository.existsByTitle(itemCreateDto.title())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Title already exists: {}", itemCreateDto.title());

                        return Mono.error(new TitleAlreadyExistsException(itemCreateDto.title()));
                    }
                    return itemRepository.save(toItem(itemCreateDto));
                })
                .map(savedItem -> {
                    log.info("Successfully created item with id: {}", savedItem.getId());
                    return itemMapper.toDto(savedItem, new HashMap<>());
                });
    }

    @Override
    public Mono<ItemDto> getItemByIdWithCartCount(Long id, Map<Long, Integer> cart) {
        return getItemDtoWithImageFromCache(id, cart);
    }

    private Mono<String> loadAndCacheImageForItem(ItemCache itemCache) {
        if (itemCache.imgPath() == null || itemCache.imgPath().isEmpty()) {
            return Mono.empty();
        }

        String imageCacheKey = buildItemImageKey(itemCache.imgPath());

        return imageService.getImageByImgPath(itemCache.imgPath())
                .filter(imageBytes -> imageBytes != null && imageBytes.length > 0)
                .flatMap(imageBytes -> {
                    String imageAsBase64 = Base64.getEncoder().encodeToString(imageBytes);
                    String imageUri = handleImageBase64ToUri(imageAsBase64, itemCache.imgPath());

                    return reactiveStringRedisTemplate.opsForValue()
                            .set(imageCacheKey, imageUri, CACHE_TTL)
                            .doOnNext(isSet -> {
                                if (Boolean.TRUE.equals(isSet)) {
                                    log.debug("Cached image for item with id: {} using key: {}",
                                            itemCache.id(), imageCacheKey);
                                } else {
                                    log.warn("Failed to cache image for item with id: {}", itemCache.id());
                                }
                            })
                            .thenReturn(imageUri);
                })
                .onErrorResume(e -> {
                    log.warn("Failed to load image for item with id: {} and message: {}",
                            itemCache.id(), e.getMessage());
                    return Mono.empty();
                });
    }


    private Mono<ItemCache> loadAndCacheItem(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .map(item -> {
                    log.debug("Loading item with id: {} from database:", id);
                    return itemMapper.toCache(item);
                })
                .flatMap(itemCache -> {
                    String cacheKey = buildItemDataKey(id);

                    return itemCacheReactiveRedisTemplate.opsForValue().set(cacheKey, itemCache, CACHE_TTL)
                            .doOnNext(isSet -> {
                                if (Boolean.TRUE.equals(isSet)) {
                                    log.debug("Cached item with id: {}", id);
                                } else {
                                    log.warn("Failed to cache item with id: {}", id);
                                }
                            })
                            .onErrorResume(e -> {
                                log.error("Cache failed for item with id: {}: {}", id, e.getMessage());

                                return Mono.empty();
                            })
                            .thenReturn(itemCache);
                });
    }

    private String handleImageBase64ToUri(String imageBase64, String imgPath) {
        return "data:image/" + imgPath.substring(imgPath.indexOf(".") + 1) + ";base64," + imageBase64;
    }

    private String buildItemDataKey(Long id) {
        return ITEM_DATA_PREFIX.formatted(id);
    }

    private String buildItemImageKey(String imgPath) {
        return ITEM_IMAGE_PREFIX.formatted(imgPath);
    }


    private List<List<ItemDto>> groupItems(List<ItemDto> content, Integer groupSize) {
        List<ItemDto> groupItems = content != null ? content : new ArrayList<>();

        return IntStream.range(0, (int) Math.ceil((double) groupItems.size() / groupSize))
                .mapToObj(i -> {
                    int fromIndex = i * groupSize;
                    int toIndex = Math.min(content.size(), fromIndex + groupSize);

                    List<ItemDto> group = new ArrayList<>(content.subList(fromIndex, toIndex));

                    while (group.size() < groupSize) {
                        group.add(createEmptyItemDto());
                    }
                    return group;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Mono<ItemDto> changeCartItemCount(CartChangeDto cartChangeDto) {
        Map<Long, Integer> cart = new HashMap<>(cartChangeDto.cartItemsCount()); // создаем копию
        Long itemId = cartChangeDto.itemId();

        log.info("Change cart count for item with id: {}, operation: {}", itemId, cartChangeDto.action());

        return cartService.changeItemCount(cartChangeDto)
                .flatMap(newCount -> {

                    cart.put(itemId, newCount);
                    log.info("Cart updated: item={}, new count={}", itemId, newCount);
                    return getItemDtoWithImageFromCache(itemId, cart);
                });
    }

    private Mono<ItemDto> getItemDtoWithImageFromCache(Long itemId, Map<Long, Integer> cart) {
        String itemCacheKey = buildItemDataKey(itemId);
        return itemCacheReactiveRedisTemplate.opsForValue().get(itemCacheKey)
                .switchIfEmpty(loadAndCacheItem(itemId))
                .flatMap(itemCache -> {

                    String imageCacheKey = buildItemImageKey(itemCache.imgPath());
                    return reactiveStringRedisTemplate.opsForValue().get(imageCacheKey)
                            .switchIfEmpty(loadAndCacheImageForItem(itemCache))
                            .map(imageUri ->
                                    itemMapper.toDtoFromItemCacheWithImage(itemCache, cart, imageUri)
                            )
                            .defaultIfEmpty(
                                    itemMapper.toDtoFromItemCacheWithoutImage(itemCache, cart)
                            );
                });
    }


    private Item toItem(ItemCreateDto dto) {
        return Item.builder().title(dto.title()).description(dto.description()).price(dto.price()).build();
    }

    private ItemDto createEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
    }
}
