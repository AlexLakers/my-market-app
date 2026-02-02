package com.alex.market.mvc.service.impl;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.output.AccountBalanceDto;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.PageDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.search.ItemSort;
import com.alex.market.mvc.search.PageItemsDto;
import com.alex.market.mvc.search.SearchDto;
import com.alex.market.mvc.service.CartService;
import com.alex.market.mvc.service.ImageService;
import com.alex.market.mvc.service.ItemCacheService;
import com.alex.market.mvc.service.PaymentApiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.awt.event.MouseMotionAdapter;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
@RequiredArgsConstructor
@Slf4j
public class ItemCacheServiceImpl implements ItemCacheService {

    private final static String ITEM_DATA_PREFIX = "item:data:%d";
    private final static String ITEM_IMAGE_PREFIX = "item:image:%s";
    private final static String ITEMS_PAGE_SET_PREFIX = "items:page:search:%1$s:sort:%2$s:page:%3$d:size:%4$s";
    private final static Integer CONTENT_GROUP_SIZE = 3;
    private final static Long GENERAL_ACCOUNT_ID = 1L;


    private final ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate;
    private final ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final ImageService imageService;
    private final CartService cartService;
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    private final PaymentApiClientService paymentApiClientService;

    @Override
    public Mono<ItemDto> getItemById(Long id, Map<Long, Integer> cart) {

        String itemCacheKey = buildItemDataKey(id);
        return itemCacheReactiveRedisTemplate.opsForValue().get(itemCacheKey)
                .switchIfEmpty(loadAndCacheItem(id))
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
                        }
                );
    }

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

    @Override
    public Mono<ItemDto> changeCartItemCount(CartChangeDto cartChangeDto) {
        Long itemId = cartChangeDto.itemId();
        Map<Long, Integer> cart = cartChangeDto.cartItemsCount();

        log.info("Change cart count for item with id: {}, operation: {}", itemId, cartChangeDto.action());

        String itemKey = buildItemDataKey(itemId);

        return itemCacheReactiveRedisTemplate.opsForValue().get(itemKey)
                .switchIfEmpty(loadAndCacheItem(itemId))
                .flatMap(itemCache -> {

                    Mono<Integer> cartOperation = cartService.changeItemCount(cartChangeDto);
                    Mono<String> imageOperation = loadAndCacheImageForItem(itemCache);


                    return Mono.zip(cartOperation, imageOperation)
                            .map(tuple -> {
                                Integer newCount = tuple.getT1();
                                String imageAsBase64Uri = tuple.getT2();


                                cart.put(itemId, newCount);

                                log.info("Cart updated: item={}, new count={}", itemId, newCount);
                                log.debug("Image loaded for item: {}", itemId);

                                return itemMapper.toDtoFromItemCacheWithImage(itemCache, cart, imageAsBase64Uri);
                            });
                })
                .doOnError(error ->
                        log.error("Error changing cart count for item {}: {}",
                                itemId, error.getMessage(), error)
                );
    }

    @Override
    public Mono<CartDto> getItemsCartWithBalanceStatus(Map<Long, Integer> cartItemsCount) {
        log.info("Getting cart with total, items count: {}", cartItemsCount != null ? cartItemsCount.size() : 0);

        if (cartItemsCount == null || cartItemsCount.isEmpty()) {
            log.debug("Empty cart, returning empty DTO");
            return Mono.just(new CartDto(List.of(), 0L, ""));
        }

        Mono<AccountBalanceDto> accountBalanceDtoMono = paymentApiClientService.getAccountById(GENERAL_ACCOUNT_ID);


        Mono<List<Item>> itemsMono = getItemsByCart(cartItemsCount).collectList();


        return itemsMono.zipWith(accountBalanceDtoMono)
                .flatMap(tuple -> {
                    List<Item> items = tuple.getT1();
                    AccountBalanceDto accDto = tuple.getT2();

                    return calculateCartDto(items, cartItemsCount, accDto);
                })
                .doOnSuccess(cartDto ->
                        log.info("Successfully calculated cart: {}", cartDto)
                )
                .doOnError(error -> log.error("Failed to calculate cart", error));
    }

    private Flux<Item> getItemsByCart(Map<Long, Integer> cartItemsCount) {

        Set<Long> ids = cartItemsCount.keySet();

        return Flux.fromIterable(ids)
                .flatMap(id -> {
                    String itemKey = buildItemDataKey(id);
                    return itemCacheReactiveRedisTemplate.opsForValue().get(itemKey)
                            .switchIfEmpty(loadAndCacheItem(id))
                            .map(itemMapper::toItem)
                            .onErrorResume(e -> {
                                log.warn("Skipping item {} due to error: {}", id, e.getMessage());
                                return Mono.empty();
                            });

                }, 10);

    }

    private Mono<CartDto> calculateCartDto(List<Item> items, Map<Long, Integer> cartItemsCount, AccountBalanceDto accountBalanceDto) {


        List<ItemDto> itemDtos = items.stream()
                .map(item -> itemMapper.toDto(item, cartItemsCount))
                .collect(Collectors.toList());

        Long amount = itemDtos.stream()
                .mapToLong(dto -> {
                    Integer count = cartItemsCount.getOrDefault(dto.id(), 0);
                    return count * dto.price();
                })
                .sum();

        String accountBalanceStatus = switch (accountBalanceDto.status()) {

            case SUCCESS -> isEnoughMoney(accountBalanceDto.balance(), amount) ? "ENOUGH" : "NOT_ENOUGH";

            case SERVICE_ERROR, NETWORK_ERROR -> "ERROR";

            default -> "UNKNOWN";

        };


        return Mono.just(new CartDto(itemDtos, amount, accountBalanceStatus));
    }

    private boolean isEnoughMoney(Long balance, Long amount) {
        return balance.compareTo(amount) >= 0;
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


    private ItemDto createEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
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
}
