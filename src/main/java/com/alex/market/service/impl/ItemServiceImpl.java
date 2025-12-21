package com.alex.market.service.impl;

import com.alex.market.api.dto.CartChangeDto;
import com.alex.market.api.dto.ItemDto;
import com.alex.market.api.dto.PageDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.model.Item;
import com.alex.market.search.ItemSort;
import com.alex.market.search.ItemSpecification;
import com.alex.market.search.PageItemsDto;
import com.alex.market.repository.ItemRepository;
import com.alex.market.search.SearchDto;
import com.alex.market.service.CartService;
import com.alex.market.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final static Integer CONTENT_GROUP_SIZE = 3;

    private final ItemRepository itemRepository;
    private final CartService cartService;

    public PageItemsDto getItemsPage(SearchDto searchDto) {

        Specification specItems = ItemSpecification.getSpecByTitleOrDescription(searchDto.search());
        Pageable pageable = PageRequest.of(
                searchDto.pageNumber() - 1,
                searchDto.pageSize(),
                ItemSort.getOrderByPriceOrTitle(searchDto.sortColumn()));

        Page<Item> pageItems = itemRepository.findAll(specItems, pageable);

        List<ItemDto> itemsDto = pageItems.getContent().stream()
                .map(it -> toItemDto(it, searchDto.cartItemsCount()))
                .collect(Collectors.toList());

        List<List<ItemDto>> groupItems = groupItems(itemsDto, CONTENT_GROUP_SIZE);

        return toPageItemsDto(searchDto, groupItems, pageable.hasPrevious(), pageItems.hasNext());

    }

    @Override
    public ItemDto findByIdWithCartCount(Long id, Map<Long, Integer> cartCountMap) {

        return itemRepository.findById(id)
                .map(it -> toItemDto(it, cartCountMap))
                .orElseThrow(() -> new ItemNotFoundException(id));

    }

    @Override
    public Integer changeCartItemCount(CartChangeDto cartChangeDto) {
        if (!itemRepository.existsById(cartChangeDto.itemId())) {
            throw new ItemNotFoundException(cartChangeDto.itemId());
        }
        return cartService.changeItemCount(cartChangeDto);
    }

    private PageItemsDto toPageItemsDto(SearchDto searchDto, List<List<ItemDto>> groupItems, boolean hasPrev, boolean hasNext) {
        return new PageItemsDto(
                groupItems,
                searchDto.search(),
                searchDto.sortColumn().name(),
                new PageDto(searchDto.pageSize(), searchDto.pageNumber(), hasPrev, hasNext));
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


    private ItemDto toItemDto(Item item, Map<Long, Integer> cart) {
       Integer count = cart.getOrDefault(item.getId(), 0);

        return new ItemDto(item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                count);

    }

    private ItemDto createEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
    }


}
