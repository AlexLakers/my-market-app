package com.alex.market.service;

import com.alex.market.api.dto.input.CartChangeDto;
import com.alex.market.api.dto.input.ItemCreateDto;
import com.alex.market.api.dto.output.ItemDto;
import com.alex.market.api.dto.output.PageDto;
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

@SpringJUnitConfig
class ItemServiceTest {

    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = Long.MAX_VALUE;
    private Map<Long, Integer> cartItemsCount;

    @Autowired
    private FileService fileService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ItemMapper itemMapper;


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
        Mockito.when(itemRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class))).thenReturn(pageItems);

        PageItemsDto actual = itemService.getItemsPage(givenDto);

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
        Mockito.when(itemRepository.findById(VALID_ID)).thenReturn(Optional.of(expectedItem));

        ItemDto actualDto = itemService.findByIdWithCartCount(VALID_ID, cartItemsCount);

        Assertions.assertThat(actualDto)
                .isNotNull()
                .hasFieldOrPropertyWithValue(Item.Fields.id, VALID_ID)
                .hasFieldOrPropertyWithValue(Item.Fields.title, expectedItem.getTitle())
                .hasFieldOrPropertyWithValue("count", cartItemsCount.get(VALID_ID));
    }

    @Test
    void findByIdWithCartCount_shouldThrowItemNotFoundException_whenIdNotFoundFail() {
        Mockito.when(itemRepository.findById(INVALID_ID)).thenReturn(Optional.empty());

        Assertions.assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> itemService.findByIdWithCartCount(INVALID_ID, cartItemsCount));

    }

    @ParameterizedTest
    @EmptySource
    void findByIdWithCartCount_shouldReturnDtoWithSetDefaultCartCount_whenCartMapNull(Map<Long, Integer> cartItemsCountNotValid) {
        Mockito.when(itemRepository.findById(VALID_ID)).thenReturn(Optional.of(new Item()));

        ItemDto actualDto = itemService.findByIdWithCartCount(VALID_ID, cartItemsCountNotValid);

        Assertions.assertThat(actualDto)
                .isNotNull()
                .hasFieldOrPropertyWithValue("count", 0);

    }

    @Test
    void changeCartItemCount_shouldCallCartServiceMethodSuccess() {
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID+1));
        Item expectedItem= new Item(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L,null);
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        Mockito.when(itemRepository.findById(VALID_ID)).thenReturn(Optional.of(expectedItem));
        Mockito.when(cartService.changeItemCount(Mockito.any(CartChangeDto.class))).thenReturn(2);

        ItemDto actualDto=itemService.changeCartItemCount(givenDto);

        Assertions.assertThat(actualDto)
                .hasFieldOrPropertyWithValue(Item.Fields.id, VALID_ID)
                .hasFieldOrPropertyWithValue("count", expectedDto.count());
    }

    @Test
    void createItem_shouldSaveItemAndReturnSavedItemDtoSuccess(){
        String TEST_IMAGE_NAME="images/test-item-test.jpg";
        String TEST_TITLE="testTitle";
        byte[] TEST_IMAGE_CONTENT="testImageContent".getBytes();
        MultipartFile mockImage = new MockMultipartFile("image", TEST_IMAGE_NAME, "image/jpeg", TEST_IMAGE_CONTENT);
        ItemCreateDto itemCreateDto = new ItemCreateDto(TEST_TITLE, "testDesc", mockImage, 1000L);
        String expectedImagePath = "images/test-item-test.jpg";
        Item expectedSavedItem = new Item(VALID_ID,TEST_TITLE,TEST_IMAGE_NAME,TEST_IMAGE_NAME,1000L,null);

        Mockito.when(itemRepository.existsByTitle(TEST_TITLE)).thenReturn(false);
        Mockito.when(fileService.saveFile(Mockito.eq(mockImage), Mockito.anyString()))
                .thenReturn(expectedImagePath);
        Mockito.when(itemRepository.save(Mockito.any(Item.class))).thenReturn(expectedSavedItem);
        ItemDto result = itemService.createItem(itemCreateDto);


        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.id()).isEqualTo(1L);
        Assertions.assertThat(result.title()).isEqualTo(TEST_TITLE);
        Assertions.assertThat(result.imgPath()).isEqualTo(expectedImagePath);

        Mockito.verify(itemRepository).existsByTitle(TEST_TITLE);
        Mockito.verify(fileService).saveFile(Mockito.eq(mockImage), Mockito.anyString());
        Mockito.verify(itemRepository).save(Mockito.any(Item.class));

    }

    @Test
    void createItem_shouldThrowTitleAlreadyExistsFail(){
        String TEST_IMAGE_NAME="images/test-item-test.jpg";
        String TEST_TITLE="testTitle";
        byte[] TEST_IMAGE_CONTENT="testImageContent".getBytes();
        MultipartFile mockImage = new MockMultipartFile("image", TEST_IMAGE_NAME, "image/jpeg", TEST_IMAGE_CONTENT);
        ItemCreateDto itemCreateDto = new ItemCreateDto(TEST_TITLE, "testDesc", mockImage, 1000L);
        Mockito.when(itemRepository.existsByTitle(TEST_TITLE)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> itemService.createItem(itemCreateDto))
                .isInstanceOf(TitleAlreadyExistsException.class)
                .hasMessageContaining(TEST_TITLE);

        Mockito.verify(itemRepository,Mockito.times(2)).existsByTitle(TEST_TITLE);
    }

        @TestConfiguration
    static class TestConfig {
        @Bean
        public CartService cartService() {
            return Mockito.mock(CartService.class);
        }

        @Bean
        public ItemRepository itemRepository() {
            return Mockito.mock(ItemRepository.class);
        }

        @Bean
        public ItemService itemService(ItemRepository itemRepository) {
            return new ItemServiceImpl(itemRepository, cartService(),fileService(),itemMapper());
        }
        @Bean
        public ItemMapper itemMapper() {
            return new ItemMapperImpl();
        }
        @Bean
        public FileService fileService() {
            return Mockito.mock(FileService.class);
        }
    }


}