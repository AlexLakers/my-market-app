package com.alex.market.mvc.service;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.mapper.ItemMapperImpl;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.search.SearchDto;
import com.alex.market.mvc.search.SortColumn;
import com.alex.market.mvc.service.impl.ItemCacheServiceImpl;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig
class ItemCacheServiceTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemCacheService itemCacheService;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate;

    @Autowired
    private ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate;

    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    private ReactiveValueOperations<String, ItemCache> itemCacheValueOps;
    private ReactiveValueOperations<String, PageInfoCache> pageInfoCacheValueOps;
    private ReactiveValueOperations<String, String> stringValueOps;

    private ItemCache testItemCache;
    private Item testItem;
    private SearchDto testSearchDto;
    private PageInfoCache testPageInfoCache;

    @BeforeEach
    void setUp() {
        itemCacheValueOps = Mockito.mock(ReactiveValueOperations.class);
        pageInfoCacheValueOps = Mockito.mock(ReactiveValueOperations.class);
        stringValueOps = Mockito.mock(ReactiveValueOperations.class);

        when(itemCacheReactiveRedisTemplate.opsForValue()).thenReturn(itemCacheValueOps);
        when(pageInfoCacheReactiveRedisTemplate.opsForValue()).thenReturn(pageInfoCacheValueOps);
        when(reactiveStringRedisTemplate.opsForValue()).thenReturn(stringValueOps);

        testItemCache = new ItemCache(1L, "Test Item", "Description", 100L, "image.jpg");
        testItem = Item.builder()
                .id(1L)
                .title("Test Item")
                .description("Description")
                .price(100L)
                .imgPath("image.jpg")
                .build();

        testSearchDto = new SearchDto(
                "test",
                SortColumn.PRICE,
                1,
                10,
                null
        );

        testPageInfoCache = new PageInfoCache(
                List.of(1L, 2L, 3L),
                10,
                1,
                false,
                true
        );
    }

    @AfterEach
    void tearDown() {
        reset(itemRepository, itemMapper, itemCacheValueOps, pageInfoCacheValueOps, stringValueOps);
    }

    @Test
    void getItemFromCache_ShouldReturnItemFromCache() {
        String expectedKey = "item:data:1";
        when(itemCacheValueOps.get(expectedKey)).thenReturn(Mono.just(testItemCache));

        StepVerifier.create(itemCacheService.getItemFromCache(1L))
                .expectNext(testItemCache)
                .verifyComplete();

        verify(itemCacheValueOps).get(expectedKey);
    }

    @Test
    void getItemFromCache_ShouldReturnEmptyWhenNotFound() {
        String expectedKey = "item:data:1";
        when(itemCacheValueOps.get(expectedKey)).thenReturn(Mono.empty());

        StepVerifier.create(itemCacheService.getItemFromCache(1L))
                .verifyComplete();

        verify(itemCacheValueOps).get(expectedKey);
    }

    @Test
    void saveItemToCache_ShouldSaveItemSuccessfully() {
        String expectedKey = "item:data:1";
        when(itemCacheValueOps.set(eq(expectedKey), eq(testItemCache), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(itemCacheService.saveItemToCache(testItemCache))
                .verifyComplete();

        verify(itemCacheValueOps).set(eq(expectedKey), eq(testItemCache), any(Duration.class));
    }

    @Test
    void getItemWithCache_ShouldReturnFromCacheWhenExists() {
        when(itemCacheValueOps.get("item:data:1")).thenReturn(Mono.just(testItemCache));

        StepVerifier.create(itemCacheService.getItemWithCache(1L))
                .expectNext(testItemCache)
                .verifyComplete();

        verify(itemCacheValueOps).get("item:data:1");
        verifyNoInteractions(itemRepository); // Репозиторий не должен вызываться
    }

    @Test
    void getItemWithCache_ShouldLoadFromDbAndCacheWhenNotInCache() {
        when(itemCacheValueOps.get("item:data:1")).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemMapper.toCache(testItem)).thenReturn(testItemCache);
        when(itemCacheValueOps.set(eq("item:data:1"), eq(testItemCache), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(itemCacheService.getItemWithCache(1L))
                .expectNext(testItemCache)
                .verifyComplete();

        verify(itemCacheValueOps).get("item:data:1");
        verify(itemRepository).findById(1L);
        verify(itemMapper).toCache(testItem);
        verify(itemCacheValueOps).set(eq("item:data:1"), eq(testItemCache), any(Duration.class));
    }

    @Test
    void getItemWithCache_ShouldReturnEmptyWhenItemNotFound() {
        when(itemCacheValueOps.get("item:data:1")).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(itemCacheService.getItemWithCache(1L))
                .verifyComplete();

        verify(itemCacheValueOps, times(1)).get("item:data:1");
        verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    void getItemsWithCache_ShouldReturnMultipleItems() {
        ItemCache itemCache2 = new ItemCache(2L, "Item 2", "Desc 2", 200L, "image2.jpg");

        when(itemCacheValueOps.get("item:data:1")).thenReturn(Mono.just(testItemCache));
        when(itemCacheValueOps.get("item:data:2")).thenReturn(Mono.just(itemCache2));

        StepVerifier.create(itemCacheService.getItemsWithCache(Set.of(1L, 2L)))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void getImageFromCache_ShouldReturnImageWhenExists() {
        String imgPath = "images/test.jpg";
        String expectedKey = "item:image:images/test.jpg";
        String expectedImageUri = "data:image/jpeg;base64,abc123";

        when(stringValueOps.get(expectedKey)).thenReturn(Mono.just(expectedImageUri));

        StepVerifier.create(itemCacheService.getImageFromCache(imgPath))
                .expectNext(expectedImageUri)
                .verifyComplete();

        verify(stringValueOps).get(expectedKey);
    }

    @Test
    void getImageFromCache_ShouldReturnEmptyWhenPathIsNull() {
        StepVerifier.create(itemCacheService.getImageFromCache(null))
                .verifyComplete();
    }

    @Test
    void saveImageToCache_ShouldSaveImageSuccessfully() {
        String imgPath = "images/test.jpg";
        String imageUri = "data:image/jpeg;base64,abc123";
        String expectedKey = "item:image:images/test.jpg";

        when(stringValueOps.set(eq(expectedKey), eq(imageUri), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(itemCacheService.saveImageToCache(imgPath, imageUri))
                .verifyComplete();

        verify(stringValueOps).set(eq(expectedKey), eq(imageUri), any(Duration.class));
    }

    @Test
    void getPageFromCache_ShouldReturnPageWhenExists() {
        String expectedKey = "items:page:search:test:sort:PRICE:page:1:size:10";
        when(pageInfoCacheValueOps.get(expectedKey)).thenReturn(Mono.just(testPageInfoCache));

        StepVerifier.create(itemCacheService.getPageFromCache(testSearchDto))
                .expectNext(testPageInfoCache)
                .verifyComplete();

        verify(pageInfoCacheValueOps).get(expectedKey);
    }

    @Test
    void savePageToCache_ShouldSavePageSuccessfully() {
        String expectedKey = "items:page:search:test:sort:PRICE:page:1:size:10";
        when(pageInfoCacheValueOps.set(eq(expectedKey), eq(testPageInfoCache), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(itemCacheService.savePageToCache(testSearchDto, testPageInfoCache))
                .verifyComplete();

        verify(pageInfoCacheValueOps).set(eq(expectedKey), eq(testPageInfoCache), any(Duration.class));
    }

    @Test
    void getPageWithCache_ShouldReturnFromCacheWhenExists() {
        Pageable pageable = PageRequest.of(0, 10);
        String expectedKey = "items:page:search:test:sort:PRICE:page:1:size:10";

        when(pageInfoCacheValueOps.get(expectedKey)).thenReturn(Mono.just(testPageInfoCache));

        StepVerifier.create(itemCacheService.getPageWithCache(testSearchDto, pageable))
                .expectNext(testPageInfoCache)
                .verifyComplete();

        verify(pageInfoCacheValueOps).get(expectedKey);
    }

    @Test
    void getPageWithCache_ShouldLoadFromDbAndCacheWhenNotInCache() {
        Pageable pageable = PageRequest.of(0, 10);
        String expectedKey = "items:page:search:test:sort:PRICE:page:1:size:10";

        Page<Item> page = new PageImpl<>(List.of(testItem), pageable, 1);

        when(pageInfoCacheValueOps.get(expectedKey)).thenReturn(Mono.empty());
        when(itemRepository.findAll(eq("test"), eq(pageable))).thenReturn(Mono.just(page));
        when(itemMapper.toCache(testItem)).thenReturn(testItemCache);
        when(pageInfoCacheValueOps.set(eq(expectedKey), any(PageInfoCache.class), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(itemCacheService.getPageWithCache(testSearchDto, pageable))
                .expectNextMatches(pageInfo ->
                        pageInfo.itemsIds().contains(1L) &&
                        pageInfo.pageSize() == 10 &&
                        pageInfo.pageNumber() == 1
                )
                .verifyComplete();

        verify(pageInfoCacheValueOps).get(expectedKey);
        verify(itemRepository).findAll(eq("test"), eq(pageable));
        verify(pageInfoCacheValueOps).set(eq(expectedKey), any(PageInfoCache.class), any(Duration.class));
    }

    @Test
    void loadItemFromDb_ShouldReturnItemCacheWhenItemExists() {
        // Замокаем всю цепочку вызовов
        when(itemCacheValueOps.get("item:data:1")).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemMapper.toCache(testItem)).thenReturn(testItemCache);
        when(itemCacheValueOps.set(eq("item:data:1"), eq(testItemCache), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(itemCacheService.getItemWithCache(1L))
                .expectNext(testItemCache)
                .verifyComplete();
    }

    @Test
    void loadItemFromDb_ShouldHandleItemNotFoundException() {
        when(itemCacheValueOps.get("item:data:1")).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(itemCacheService.getItemWithCache(1L))
                .verifyComplete();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ItemRepository itemRepository() {
            return mock(ItemRepository.class);
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
        public ItemMapper itemMapper() {
            return Mockito.mock(ItemMapper.class);
        }

        @Bean
        public ItemCacheService itemCacheService(ItemRepository itemRepository,
                                                 ItemMapper itemMapper,
                                                 ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate,
                                                 ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate,
                                                 ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
            return new ItemCacheServiceImpl(
                    itemCacheReactiveRedisTemplate,
                    pageInfoCacheReactiveRedisTemplate,
                    reactiveStringRedisTemplate,
                    itemRepository,
                    itemMapper
            );
        }
    }
}