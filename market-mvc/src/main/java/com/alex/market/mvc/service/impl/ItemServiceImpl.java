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
import com.alex.market.mvc.service.ItemCacheService;
import com.alex.market.mvc.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CartService cartService;
    private final ImageService imageService;
    private final ItemCacheService itemCacheService;

    @Override
    public Mono<PageItemsDto> getItemsPage(SearchDto searchDto) {
        log.info("Getting page with items: {}", searchDto);

        Pageable pageable = PageRequest.of(
                searchDto.pageNumber() - 1,
                searchDto.pageSize(),
                ItemSort.getOrderByPriceOrTitle(searchDto.sortColumn()));

        return itemCacheService.getPageWithCache(searchDto, pageable)
                .flatMap(pageInfoCache ->
                        itemCacheService.getItemsWithCache(new HashSet<>(pageInfoCache.itemsIds()))
                                .collectList()
                )
                .map(itemCaches -> toPageItemsDto(searchDto, itemCaches))
                .doOnNext(result ->
                        log.info("Successful return page of items in {} groups", result.items().size())
                );
    }

    private PageItemsDto toPageItemsDto(SearchDto searchDto, List<ItemCache> itemCaches) {
        List<ItemCache> validCaches = itemCaches.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ItemDto> itemDtos = validCaches.stream()
                .map(cache -> itemMapper.toDtoFromItemCacheWithoutImage(
                        cache,
                        searchDto.cartItemsCount())
                )
                .collect(Collectors.toList());

        List<List<ItemDto>> groupedItems = groupItems(itemDtos, CONTENT_GROUP_SIZE);

        boolean hasPrevious = searchDto.pageNumber() > 1;
        boolean hasNext = !itemCaches.isEmpty();

        return new PageItemsDto(
                groupedItems,
                searchDto.search(),
                searchDto.sortColumn().name(),
                new PageDto(searchDto.pageSize(), searchDto.pageNumber(), hasPrevious, hasNext)
        );
    }

    @Override
    @Transactional
    public Mono<ItemDto> createItem(ItemCreateDto itemCreateDto) {
        log.info("Creating item with: title={}, price={}", itemCreateDto.title(), itemCreateDto.price());

        return itemRepository.save(toItem(itemCreateDto))
                .map(savedItem -> {
                    log.info("Successfully created item with id: {}", savedItem.getId());
                    return itemMapper.toDto(savedItem, new HashMap<>());
                })
                .onErrorResume(DataIntegrityViolationException.class, e -> {
                    log.warn("Title already exists: {}", itemCreateDto.title());
                    return Mono.error(new TitleAlreadyExistsException(itemCreateDto.title()));
                });
    }

    @Override
    public Mono<ItemDto> getItemByIdWithCartCount(Long id, Map<Long, Integer> cart) {
        log.debug("Getting item with cart count: id={}", id);

        return itemCacheService.getItemWithCache(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .flatMap(itemCache -> {
                    if (itemCache.imgPath() == null || itemCache.imgPath().isEmpty()) {
                        return Mono.just(itemMapper.toDtoFromItemCacheWithoutImage(itemCache, cart));
                    }

                    return itemCacheService.getImageFromCache(itemCache.imgPath())
                            .switchIfEmpty(Mono.defer(() ->
                                    loadImageFromService(itemCache)
                                            .flatMap(imageUri ->
                                                    itemCacheService.saveImageToCache(itemCache.imgPath(), imageUri)
                                                            .thenReturn(imageUri)
                                            )
                            ))
                            .map(imageUri ->
                                    itemMapper.toDtoFromItemCacheWithImage(itemCache, cart, imageUri)
                            )
                            .defaultIfEmpty(
                                    itemMapper.toDtoFromItemCacheWithoutImage(itemCache, cart)
                            );
                });
    }

    private Mono<String> loadImageFromService(ItemCache itemCache) {
        return imageService.getImageByImgPath(itemCache.imgPath())
                .filter(imageBytes -> imageBytes != null && imageBytes.length > 0)
                .map(imageBytes -> Base64.getEncoder().encodeToString(imageBytes))
                .map(imageBase64 -> handleImageBase64ToUri(imageBase64, itemCache.imgPath()))
                .onErrorResume(e -> {
                    log.warn("Failed to load image for item: {}", itemCache.id(), e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<ItemDto> changeCartItemCount(CartChangeDto cartChangeDto) {
        Map<Long, Integer> cart = new HashMap<>(cartChangeDto.cartItemsCount());
        Long itemId = cartChangeDto.itemId();

        log.info("Change cart count: item={}, action={}", itemId, cartChangeDto.action());

        return cartService.changeItemCount(cartChangeDto)
                .flatMap(newCount -> {
                    cart.put(itemId, newCount);
                    return getItemByIdWithCartCount(itemId, cart);
                });
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

    private String handleImageBase64ToUri(String imageBase64, String imgPath) {
        String extension = imgPath.substring(imgPath.lastIndexOf(".") + 1);
        return "data:image/" + extension + ";base64," + imageBase64;
    }

    private Item toItem(ItemCreateDto dto) {
        return Item.builder()
                .title(dto.title())
                .description(dto.description())
                .price(dto.price())
                .build();
    }

    private ItemDto createEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
    }
}
