package com.alex.market.service.impl;


import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.exception.TitleAlreadyExistsException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.search.ItemSort;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.service.CartService;
import com.alex.market.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    public Mono<PageItemsDto> getItemsPage(SearchDto searchDto) {
        log.info("Getting page with items: pageNumber={}, pageSize={}, sortColumn={}, search={}, cartItemsCount={}",
                searchDto.pageNumber(),
                searchDto.pageSize(),
                searchDto.sortColumn(),
                searchDto.search(),
                searchDto.cartItemsCount());

        Pageable pageable = PageRequest.of(
                searchDto.pageNumber() - 1,
                searchDto.pageSize(),
                ItemSort.getOrderByPriceOrTitle(searchDto.sortColumn()));

        return itemRepository.findAll(searchDto.search(), pageable)
                .map(page -> {
                    List<ItemDto> itemDtoList = page.getContent().stream()
                            .map(item -> itemMapper.toDto(item, searchDto.cartItemsCount())).collect(Collectors.toList());

                    List<List<ItemDto>> groupItems = groupItems(itemDtoList, CONTENT_GROUP_SIZE);
                    return toPageItemsDto(searchDto, groupItems, pageable.hasPrevious(), page.hasNext());
                }).doOnNext(result ->
                        log.info("Successful return page of items: {} groups", result.items().size())
                )
                .doOnError(error ->
                        log.error("Error during handing error: {}", error.getMessage(), error)
                );
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
    public Mono<ItemDto> getItemByIdWithCartCount(Long id, Map<Long, Integer> cartCountMap) {
        log.info("Get item by id: {}, cart size: {}", id,
                cartCountMap != null ? cartCountMap.size() : 0);

        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .map(it -> {
                    log.debug("Found item: {}", it.getTitle());
                    return itemMapper.toDto(it, cartCountMap);
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

    @Override
    public Mono<ItemDto> changeCartItemCount(CartChangeDto cartChangeDto) {
        Map<Long, Integer> cart = cartChangeDto.cartItemsCount();
        Long itemId = cartChangeDto.itemId();
        log.info("Change cart count for item with id: {}, operation: {}", itemId, cartChangeDto.action());

        return itemRepository.findById(cartChangeDto.itemId())
                .switchIfEmpty(Mono.defer(() -> {

                    log.warn("Item with id: {} not found for cart update", itemId);
                    return Mono.error(new ItemNotFoundException(itemId));
                }))
                .flatMap(item ->
                        cartService.changeItemCount(cartChangeDto)
                                .map(newCount -> {
                                    cart.put(itemId, newCount);

                                    log.info("Cart updated: item={}, new count={}", itemId, newCount);
                                    return itemMapper.toDto(item, cart);
                                }))
                .doOnError(error -> {
                    if (error instanceof ItemNotFoundException) {
                        log.warn("Cannot update cart: item with id {} not found", itemId);
                    } else {
                        log.error("Cart update failed for item with id{}: {}", itemId, error.getMessage());
                    }
                });

    }

    private PageItemsDto toPageItemsDto(SearchDto searchDto, List<List<ItemDto>> groupItems, boolean hasPrev, boolean hasNext) {
        return new PageItemsDto(
                groupItems,
                searchDto.search(),
                searchDto.sortColumn().name(),
                new PageDto(searchDto.pageSize(), searchDto.pageNumber(), hasPrev, hasNext));
    }

    private Item toItem(ItemCreateDto dto) {
        return Item.builder().title(dto.title()).description(dto.description()).price(dto.price()).build();
    }

    private ItemDto createEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
    }
}
