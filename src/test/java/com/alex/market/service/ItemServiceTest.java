package com.alex.market.service;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.exception.TitleAlreadyExistsException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.mapper.ItemMapperImpl;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.impl.ItemServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class ItemServiceTest {

    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = Long.MAX_VALUE;
    private Map<Long, Integer> cartItemsCount;

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemService itemService;



    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        cartItemsCount.put(2L, 2);
        cartItemsCount.put(3L, 3);
    }

    @Test
    void getItemsPage_shouldReturnItems() {
        SearchDto givenDto = new SearchDto("test", SortColumn.PRICE, 1, 3, cartItemsCount);
        Page<Item> pageItems = getExpectedPageItems(givenDto.pageNumber(), givenDto.pageSize(), 4);
        PageItemsDto pageItemsDto = getExpectedPageItemsDto(givenDto.search(), givenDto.sortColumn().name(), pageItems.getNumber(), pageItems.getSize(), pageItems.hasPrevious(), pageItems.hasNext());
        when(itemRepository.findAll(Mockito.anyString(),Mockito.any(Pageable.class))).thenReturn(Mono.just(pageItems));

        PageItemsDto actual = itemService.getItemsPage(givenDto).block();

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

    static Page<Item> getExpectedPageItems(int pageNumber, int pageSize, long total) {
        List<Item> content = Arrays.asList(
                new Item(1L, "testTitle1", "testDesc1", "testImagePath1", 1000L),
                new Item(2L, "testTitle2", "testDesc2", "testImagePath2", 2000L),
                new Item(3L, "testTitle3", "testDesc3", "testImagePath3", 3000L),
                new Item(4L, "testTitle4", "testDesc4", "testImagePath4", 4000L)
        );

        return new PageImpl<Item>(content, PageRequest.of(pageNumber, pageSize), total);
    }

    @Test
    void createItem_shouldReturnSavedItemWithIdSuccess() {
        Mockito.when(itemRepository.existsByTitle("test-title")).thenReturn(Mono.just(Boolean.FALSE));
        Mockito.when(itemRepository.save(Mockito.any(Item.class))).thenReturn(Mono.just(Item.builder().id(VALID_ID).build()));
        ItemCreateDto givenDto=new ItemCreateDto("test-title","description",1000L);
        ItemDto actualSavedItemDto=itemService.createItem(givenDto).block();

        Assertions.assertThat(actualSavedItemDto).isNotNull()
                .hasFieldOrPropertyWithValue(Item.Fields.id,VALID_ID);
    }
    @Test
    void createItem_shouldThrowTitleAlreadyExistsException_whenTitleAlreadyExistsFail() {
        Mockito.when(itemRepository.existsByTitle("already-title")).thenReturn(Mono.just(Boolean.TRUE));
        ItemCreateDto givenDto=new ItemCreateDto("already-title","description",1000L);

        Assertions.assertThatExceptionOfType(TitleAlreadyExistsException.class)
                .isThrownBy(()->itemService.createItem(givenDto).block());
    }

    @TestConfiguration
    static class TestConfig {
   /*     @Bean
        public CartService cartService() {
            return mock(CartService.class);
        }*/

        @Bean
        public ItemRepository itemRepository() {
            return mock(ItemRepository.class);
        }

        @Bean
        public ItemService itemService(ItemRepository itemRepository) {
            return new ItemServiceImpl(itemRepository,itemMapper());
        }
        @Bean
        public ItemMapper itemMapper() {
            return new ItemMapperImpl();
        }

    }
}