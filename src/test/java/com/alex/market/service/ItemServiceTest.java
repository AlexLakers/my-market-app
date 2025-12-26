package com.alex.market.service;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.exception.TitleAlreadyExistsException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.mapper.ItemMapperImpl;
import com.alex.market.model.CartAction;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.search.*;
import com.alex.market.service.impl.ItemServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.multipart.MultipartFile;


import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
class ItemServiceTest {

    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = Long.MAX_VALUE;
    private Map<Long, Integer> cartItemsCount;

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;


    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        cartItemsCount.put(2L, 2);
        cartItemsCount.put(3L, 3);
    }

    @Test
    void getItemsPage_shouldReturnItemsPage() {
        SearchDto givenDto = new SearchDto("test", SortColumn.PRICE, 1, 3, cartItemsCount);
        Page<Item> pageItems = getExpectedPageItems(givenDto.pageNumber(), givenDto.pageSize(), 4);
        PageItemsDto pageItemsDto = getExpectedPageItemsDto(givenDto.search(), givenDto.sortColumn().name(), pageItems.getNumber(), pageItems.getSize(), pageItems.hasPrevious(), pageItems.hasNext());
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageItems);

        PageItemsDto actual = itemService.getItemsPage(givenDto);

        assertThat(actual.items())
                .isNotNull()
                .hasSize(pageItemsDto.items().size());
        assertThat(actual.items().getFirst())
                .isNotNull()
                .hasSize(3)
                .contains(pageItemsDto.items().getFirst().getFirst());
        assertThat(actual.items().getLast())
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
                new Item(1L, "testTitle1", "testDesc1", "testImagePath1", 1000L, null),
                new Item(2L, "testTitle2", "testDesc2", "testImagePath2", 2000L, null),
                new Item(3L, "testTitle3", "testDesc3", "testImagePath3", 3000L, null),
                new Item(4L, "testTitle4", "testDesc4", "testImagePath4", 4000L, null)
        );

        return new PageImpl<Item>(content, PageRequest.of(pageNumber, pageSize), total);
    }

    @Test
    void findByIdWithCartCount_shouldReturnDtoSuccess() {
        Item expectedItem = new Item(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, null);
        when(itemRepository.findById(VALID_ID)).thenReturn(Optional.of(expectedItem));

        ItemDto actualDto = itemService.findByIdWithCartCount(VALID_ID, cartItemsCount);

        assertThat(actualDto)
                .isNotNull()
                .hasFieldOrPropertyWithValue(Item.Fields.id, VALID_ID)
                .hasFieldOrPropertyWithValue(Item.Fields.title, expectedItem.getTitle())
                .hasFieldOrPropertyWithValue("count", cartItemsCount.get(VALID_ID));
    }

    @Test
    void findByIdWithCartCount_shouldThrowItemNotFoundException_whenIdNotFoundFail() {
        when(itemRepository.findById(INVALID_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> itemService.findByIdWithCartCount(INVALID_ID, cartItemsCount));

    }

    @ParameterizedTest
    @EmptySource
    void findByIdWithCartCount_shouldReturnDtoWithSetDefaultCartCount_whenCartMapNull(Map<Long, Integer> cartItemsCountNotValid) {
        when(itemRepository.findById(VALID_ID)).thenReturn(Optional.of(new Item()));

        ItemDto actualDto = itemService.findByIdWithCartCount(VALID_ID, cartItemsCountNotValid);

        assertThat(actualDto)
                .isNotNull()
                .hasFieldOrPropertyWithValue("count", 0);

    }

    @Test
    void changeCartItemCount_shouldCallCartServiceMethodSuccess() {
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID+1));
        Item expectedItem= new Item(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L,null);
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        when(itemRepository.findById(VALID_ID)).thenReturn(Optional.of(expectedItem));
        when(cartService.changeItemCount(any(CartChangeDto.class))).thenReturn(2);

        ItemDto actualDto=itemService.changeCartItemCount(givenDto);

        assertThat(actualDto)
                .hasFieldOrPropertyWithValue(Item.Fields.id, VALID_ID)
                .hasFieldOrPropertyWithValue("count", expectedDto.count());
    }

        @TestConfiguration
    static class TestConfig {
        @Bean
        public CartService cartService() {
            return mock(CartService.class);
        }

        @Bean
        public ItemRepository itemRepository() {
            return mock(ItemRepository.class);
        }

        @Bean
        public ItemService itemService(ItemRepository itemRepository) {
            return new ItemServiceImpl(itemRepository, cartService(),itemMapper());
        }
        @Bean
        public ItemMapper itemMapper() {
            return new ItemMapperImpl();
        }

    }


}