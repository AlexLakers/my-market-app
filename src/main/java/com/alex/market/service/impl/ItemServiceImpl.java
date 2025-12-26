package com.alex.market.service.impl;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.exception.TitleAlreadyExistsException;
import com.alex.market.mapper.ItemMapper;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final static Integer CONTENT_GROUP_SIZE = 3;

    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemMapper itemMapper;

    public PageItemsDto getItemsPage(SearchDto searchDto) {

        Specification specItems = ItemSpecification.getSpecByTitleOrDescription(searchDto.search());
        Pageable pageable = PageRequest.of(
                searchDto.pageNumber() - 1,
                searchDto.pageSize(),
                ItemSort.getOrderByPriceOrTitle(searchDto.sortColumn()));

        Page<Item> pageItems = itemRepository.findAll(specItems, pageable);

        List<ItemDto> itemsDto = pageItems.getContent().stream()
                .map(it -> itemMapper.toDto(it, searchDto.cartItemsCount()))
                .collect(Collectors.toList());

        List<List<ItemDto>> groupItems = groupItems(itemsDto, CONTENT_GROUP_SIZE);

        return toPageItemsDto(searchDto, groupItems, pageable.hasPrevious(), pageItems.hasNext());

    }

    @Override
    public ItemDto findByIdWithCartCount(Long id, Map<Long, Integer> cartCountMap) {

        return itemRepository.findById(id)
                .map(it -> itemMapper.toDto(it, cartCountMap))
                .orElseThrow(() -> new ItemNotFoundException(id));

    }

    @Override
    public ItemDto changeCartItemCount(CartChangeDto cartChangeDto) {
        Map<Long, Integer> cart = cartChangeDto.cartItemsCount();
        Long itemId = cartChangeDto.itemId();
        return itemRepository.findById(cartChangeDto.itemId())
                .map(it -> {
                    cart.put(itemId, cartService.changeItemCount(cartChangeDto));
                    return itemMapper.toDto(it, cart);
                })
                .orElseThrow(() -> new ItemNotFoundException(itemId));
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

    @Override
    public ItemDto createItem(ItemCreateDto itemCreateDto) {

        if(itemRepository.existsByTitle(itemCreateDto.title())) {
            throw new TitleAlreadyExistsException(itemCreateDto.title());
        }
        Item savedItem=itemRepository.save(toItem(itemCreateDto/*, imagePath*/));
        return itemMapper.toDto(savedItem,new HashMap<>());
    }
    private Item toItem(ItemCreateDto dto){
        return Item.builder().title(dto.title()).description(dto.description()).price(dto.price()).build();
    }

    private ItemDto createEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", 0L, 0);
    }
}
