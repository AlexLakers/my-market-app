package com.alex.market.mvc.service;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.input.ItemCreateDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.PageDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.exception.TitleAlreadyExistsException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.mapper.ItemMapperImpl;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.search.PageItemsDto;
import com.alex.market.mvc.search.SearchDto;
import com.alex.market.mvc.search.SortColumn;
import com.alex.market.mvc.service.impl.ItemServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class ItemServiceTest {

    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = 10000L;
    private Map<Long, Integer> cartItemsCount;

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ImageService imageService;

    @Autowired
    private ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate;
    @Autowired
    private ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate;
    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;


    private ReactiveValueOperations<String, ItemCache> valueOperationsItem;

    private ReactiveValueOperations<String, PageInfoCache> valueOperationsPage;

    private ReactiveValueOperations<String,String> valueOperationsImage;


    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        cartItemsCount.put(2L, 2);
        cartItemsCount.put(3L, 3);

        valueOperationsItem = Mockito.mock(ReactiveValueOperations.class);
        valueOperationsPage = Mockito.mock(ReactiveValueOperations.class);
        valueOperationsImage = Mockito.mock(ReactiveValueOperations.class);


        when(itemCacheReactiveRedisTemplate.opsForValue()).thenReturn(valueOperationsItem);
        when(pageInfoCacheReactiveRedisTemplate.opsForValue()).thenReturn(valueOperationsPage);
        when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperationsImage);
    }

    @Test
    void getItemsPage_shouldReturnItems() {

        SearchDto givenDto = new SearchDto("test", SortColumn.PRICE, 1, 3, cartItemsCount);
        Page<Item> pageItems = getExpectedPageItems(givenDto.pageNumber(), givenDto.pageSize(), 4);


        ItemCache itemCache1 = new ItemCache(1L, "testTitle1", "testDesc1", 1000L, "testImagePath1");
        ItemCache itemCache2 = new ItemCache(2L, "testTitle2", "testDesc2", 2000L, "testImagePath2");
        ItemCache itemCache3 = new ItemCache(3L, "testTitle3", "testDesc3", 3000L, "testImagePath3");
        ItemCache itemCache4 = new ItemCache(4L, "testTitle4", "testDesc4", 4000L, "testImagePath4");

        when(itemRepository.findAll(anyString(), any(Pageable.class)))
                .thenReturn(Mono.just(pageItems));


        when(valueOperationsPage.get(anyString()))
                .thenReturn(Mono.empty());
        when(valueOperationsPage.set(anyString(), any(PageInfoCache.class), any()))
                .thenReturn(Mono.just(true));


        when(valueOperationsItem.get("item:data:1")).thenReturn(Mono.just(itemCache1));
        when(valueOperationsItem.get("item:data:2")).thenReturn(Mono.just(itemCache2));
        when(valueOperationsItem.get("item:data:3")).thenReturn(Mono.just(itemCache3));
        when(valueOperationsItem.get("item:data:4")).thenReturn(Mono.just(itemCache4));

        when(valueOperationsItem.set(anyString(), any(ItemCache.class), any()))
                .thenReturn(Mono.just(true));

        Mono<PageItemsDto> result = itemService.getItemsPage(givenDto);

        StepVerifier.create(result)
                .expectNextMatches(pageDto -> {

                    assertThat(pageDto).isNotNull();
                    assertThat(pageDto.items()).isNotNull();


                    return pageDto.pageDto().pageNumber() == 1 &&
                           pageDto.pageDto().pageSize() == 3;
                })
                .verifyComplete();
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
        Mockito.when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(Item.builder().id(VALID_ID).build()));
        ItemCreateDto givenDto = new ItemCreateDto("test-title", "description", 1000L);
        ItemDto actualSavedItemDto = itemService.createItem(givenDto).block();

        Assertions.assertThat(actualSavedItemDto).isNotNull()
                .hasFieldOrPropertyWithValue(Item.Fields.id, VALID_ID);
    }

    @Test
    void createItem_shouldThrowTitleAlreadyExistsException_whenTitleAlreadyExistsFail() {
        Mockito.when(itemRepository.existsByTitle("already-title")).thenReturn(Mono.just(Boolean.TRUE));
        ItemCreateDto givenDto = new ItemCreateDto("already-title", "description", 1000L);

        Assertions.assertThatExceptionOfType(TitleAlreadyExistsException.class)
                .isThrownBy(() -> itemService.createItem(givenDto).block());
    }

    @Test
    void getItemByIdWithCart_shouldReturnDtoWithIdSuccess() {
        ItemCache itemCache1 = new ItemCache(1L, "testTitle1", "testDesc1", 1000L, "testImagePath1");
        Item item = Item.builder().id(VALID_ID).build();
        when(itemRepository.findById(VALID_ID)).thenReturn(Mono.just(item));

        when(valueOperationsItem.get("item:data:1")).thenReturn(Mono.just(itemCache1));
        when(valueOperationsImage.get("item:image:testImagePath1"))
                .thenReturn(Mono.just(Base64.getEncoder().encodeToString(new byte[]{1,2,3,4})));
        when(valueOperationsItem.set(anyString(), any(ItemCache.class), any()))
                .thenReturn(Mono.just(true));
        when(imageService.getImageByImgPath(Mockito.anyString())).thenReturn(Mono.just(new byte[]{1,2,3,4}));

        ItemDto actualDto = itemService.getItemByIdWithCartCount(VALID_ID, cartItemsCount).block();
        Assertions.assertThat(actualDto).isNotNull()
                .hasFieldOrPropertyWithValue(Item.Fields.id, VALID_ID);

    }

    @Test
    void getItemByIdWithCartCount_shouldThrowItemNotFoundException_whenItemNotFoundFail() {
        when(valueOperationsItem.get("item:data:10000")).thenReturn(Mono.empty());
        when(itemRepository.findById(INVALID_ID)).thenReturn(Mono.empty());

        Assertions.assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> itemService.getItemByIdWithCartCount(INVALID_ID, cartItemsCount).block());
    }

    @Test
    void changeCartItemCount_shouldCallCartServiceMethodSuccess() {
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID + 1));
        Item expectedItem = new Item(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L);
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);

        Mockito.when(itemRepository.findById(VALID_ID)).thenReturn(Mono.just(expectedItem));
        Mockito.when(cartService.changeItemCount(any(CartChangeDto.class))).thenReturn(Mono.just(2));

        ItemDto actualDto = itemService.changeCartItemCount(givenDto).block();

        assertThat(actualDto)
                .hasFieldOrPropertyWithValue(Item.Fields.id, VALID_ID)
                .hasFieldOrPropertyWithValue("count", expectedDto.count());
    }


    @TestConfiguration
    static class TestConfig {

        @Bean
        public ItemRepository itemRepository() {
            return mock(ItemRepository.class);
        }

        @Bean
        public CartService cartService() {
            return mock(CartService.class);
        }

        @Bean
        public ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate() {
            return Mockito.mock(ReactiveRedisTemplate.class);
        }

        @Bean
        public ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate() {
            return Mockito.mock(ReactiveRedisTemplate.class);
        }

        @Bean
        public ReactiveStringRedisTemplate reactiveStringRedisTemplate() {
            return Mockito.mock(ReactiveStringRedisTemplate.class);
        }


        @Bean
        public ImageService imageService() {
            return Mockito.mock(ImageService.class);
        }

        @Bean
        public ItemService itemService(ItemRepository itemRepository,
                                       ItemMapper itemMapper,
                                       CartService cartService,
                                       ImageService imageService,
                                       ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate,
                                       ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate,
                                       ReactiveStringRedisTemplate reactiveStringRedisTemplate

        ) {
            return new ItemServiceImpl(itemRepository, itemMapper, cartService, imageService, itemCacheReactiveRedisTemplate, pageInfoCacheReactiveRedisTemplate, reactiveStringRedisTemplate);
        }

        @Bean
        public ItemMapper itemMapper() {
            return new ItemMapperImpl();
        }

    }
}