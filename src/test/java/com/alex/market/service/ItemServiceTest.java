package com.alex.market.service;

import com.alex.market.api.dto.ItemDto;
import com.alex.market.api.dto.PageDto;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.search.*;
import com.alex.market.service.impl.ItemServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;


import java.util.*;

@SpringJUnitConfig
class ItemServiceTest {

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemService itemService;

    @Test
    void getItemsPage_shouldReturnItemsPage() {
        Map<Long, Integer> cartItemsCount = Map.of(1L,1,2L,2,3L,3,4L,4);
        SearchDto givenDto = new SearchDto("test", SortColumn.PRICE, 1, 3, cartItemsCount);
        Page<Item> pageItems=getExpectedPageItems(givenDto.pageNumber(), givenDto.pageSize(),4);
        Specification itemSpec = ItemSpecification.getSpecByTitleOrDescription(givenDto.search());
        Sort itemSort = ItemSort.getOrderByPriceOrTitle(givenDto.sortColumn());
        PageItemsDto pageItemsDto=getExpectedPageItemsDto(givenDto.search(), givenDto.sortColumn().name(), pageItems.getNumber(), pageItems.getSize(), pageItems.hasPrevious(), pageItems.hasNext());
        Mockito.when(itemRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class))).thenReturn(pageItems);

        PageItemsDto actual=itemService.getItemsPage(givenDto);

        Assertions.assertThat(actual.items())
                .isNotNull()
                .hasSize(pageItemsDto.items().size());
        Assertions.assertThat(actual.items().getFirst())
                .isNotNull()
                .hasSize(3)
                .contains(pageItemsDto.items().getFirst().getFirst());
        Assertions.assertThat(actual.items().getLast())
                .isNotNull()
                .hasSize(3)
                .contains(pageItemsDto.items().getLast().getLast());
    }


    static PageItemsDto getExpectedPageItemsDto(String search, String sort, Integer page, Integer size, boolean hasPrev, boolean hasNext) {

        ItemDto itemDto1 = new ItemDto(1L, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        ItemDto itemDto2 = new ItemDto(2L, "testTitle2", "testDesc2", "testImagePath2", 2000L, 1);
        ItemDto itemDto3 = new ItemDto(3L, "testTitle3", "testDesc3", "testImagePath3", 3000L, 1);
        ItemDto itemDto4 = new ItemDto(4L, "testTitle4", "testDesc4", "testImagePath4", 4000L, 1);
        ItemDto emptyItemDto = new ItemDto(-1L, "", "", "", 0L, 0);
        List<List<ItemDto>> expectedItemsDto = List.of(
                List.of(itemDto1, itemDto2, itemDto3),
                List.of(itemDto4, emptyItemDto, emptyItemDto)
        );

        return new PageItemsDto(expectedItemsDto, search, sort, new PageDto(size, page, hasPrev, hasNext));
    }

    static Page<Item> getExpectedPageItems( int pageNumber,int pageSize, long total) {
         List<Item> content=   Arrays.asList(
                new Item(1L, "testTitle1", "testDesc1", "testImagePath1", 1000L, null),
                new Item(2L, "testTitle2", "testDesc2", "testImagePath2", 2000L, null),
                new Item(3L, "testTitle3", "testDesc3", "testImagePath3", 3000L, null),
                new Item(4L, "testTitle4", "testDesc4", "testImagePath4", 4000L, null)
        );

         return new PageImpl<Item>(content, PageRequest.of(pageNumber,pageSize), total);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ItemRepository itemRepository() {
            return Mockito.mock(ItemRepository.class);
        }

        @Bean
        public ItemService itemService(ItemRepository itemRepository) {
            return new ItemServiceImpl(itemRepository);
        }
    }


}