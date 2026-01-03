package com.alex.market.service.impl;


import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.repository.ItemRepository;
import com.alex.market.search.ItemSort;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final static Integer CONTENT_GROUP_SIZE = 3;

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;


    public Mono<PageItemsDto> getItemsPage(SearchDto searchDto) {
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

    private PageItemsDto toPageItemsDto(SearchDto searchDto, List<List<ItemDto>> groupItems, boolean hasPrev, boolean hasNext) {
        return new PageItemsDto(
                groupItems,
                searchDto.search(),
                searchDto.sortColumn().name(),
                new PageDto(searchDto.pageSize(), searchDto.pageNumber(), hasPrev, hasNext));
    }

    private ItemDto createEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
    }
}
