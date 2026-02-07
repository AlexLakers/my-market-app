package com.alex.market.mvc.service;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.input.ItemCreateDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.exception.TitleAlreadyExistsException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.search.PageItemsDto;
import com.alex.market.mvc.search.SearchDto;
import com.alex.market.mvc.search.SortColumn;
import com.alex.market.mvc.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private CartService cartService;

    @Mock
    private ImageService imageService;

    @Mock
    private ItemCacheService itemCacheService;

    @InjectMocks
    private ItemServiceImpl itemService;

    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = 10000L;
    private Map<Long, Integer> cartItemsCount;
    private ItemCache testItemCache;
    private Item testItem;
    private ItemDto testItemDto;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        cartItemsCount.put(2L, 2);
        cartItemsCount.put(3L, 3);

        testItemCache = new ItemCache(
                VALID_ID,
                "Test Item",
                "Description",
                1000L,
                "testImagePath.jpg"
        );

        testItem = Item.builder()
                .id(VALID_ID)
                .title("Test Item")
                .description("Description")
                .price(1000L)
                .imgPath("testImagePath.jpg")
                .build();

        testItemDto = new ItemDto(
                VALID_ID,
                "Test Item",
                "Description",
                "data:image/jpeg;base64,abc123",
                1000L,
                1
        );
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(itemRepository, itemMapper, cartService, imageService, itemCacheService);
    }

    @Test
    void getItemsPage_shouldReturnItems() {
        SearchDto givenDto = new SearchDto("test", SortColumn.PRICE, 1, 3, cartItemsCount);

        PageInfoCache pageInfoCache = new PageInfoCache(
                Arrays.asList(1L, 2L, 3L, 4L),
                3,
                1,
                false,
                true
        );

        ItemCache itemCache1 = new ItemCache(1L, "testTitle1", "testDesc1", 1000L, "testImagePath1");
        ItemCache itemCache2 = new ItemCache(2L, "testTitle2", "testDesc2", 2000L, "testImagePath2");
        ItemCache itemCache3 = new ItemCache(3L, "testTitle3", "testDesc3", 3000L, "testImagePath3");
        ItemCache itemCache4 = new ItemCache(4L, "testTitle4", "testDesc4", 4000L, "testImagePath4");

        ItemDto itemDto1 = new ItemDto(1L, "testTitle1", "testDesc1", "", 1000L, 1);
        ItemDto itemDto2 = new ItemDto(2L, "testTitle2", "testDesc2", "", 2000L, 2);
        ItemDto itemDto3 = new ItemDto(3L, "testTitle3", "testDesc3", "", 3000L, 3);
        ItemDto itemDto4 = new ItemDto(4L, "testTitle4", "testDesc4", "", 4000L, 0);

        when(itemCacheService.getPageWithCache(eq(givenDto), any(Pageable.class)))
                .thenReturn(Mono.just(pageInfoCache));

        when(itemCacheService.getItemsWithCache(new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L))))
                .thenReturn(Flux.just(itemCache1, itemCache2, itemCache3, itemCache4));

        when(itemMapper.toDtoFromItemCacheWithoutImage(itemCache1, cartItemsCount))
                .thenReturn(itemDto1);
        when(itemMapper.toDtoFromItemCacheWithoutImage(itemCache2, cartItemsCount))
                .thenReturn(itemDto2);
        when(itemMapper.toDtoFromItemCacheWithoutImage(itemCache3, cartItemsCount))
                .thenReturn(itemDto3);
        when(itemMapper.toDtoFromItemCacheWithoutImage(itemCache4, cartItemsCount))
                .thenReturn(itemDto4);

        StepVerifier.create(itemService.getItemsPage(givenDto))
                .expectNextMatches(pageDto -> {
                    assertThat(pageDto).isNotNull();
                    assertThat(pageDto.items()).isNotNull();
                    assertThat(pageDto.pageDto().pageNumber()).isEqualTo(1);
                    assertThat(pageDto.pageDto().pageSize()).isEqualTo(3);
                    assertThat(pageDto.search()).isEqualTo("test");
                    assertThat(pageDto.sort()).isEqualTo("PRICE");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void createItem_shouldReturnSavedItemWithIdSuccess() {
        ItemCreateDto givenDto = new ItemCreateDto("test-title", "description", 1000L);
        Item savedItem = Item.builder()
                .id(VALID_ID)
                .title("test-title")
                .description("description")
                .price(1000L)
                .build();
        ItemDto expectedItemDto = new ItemDto(VALID_ID, "test-title", "description", "", 1000L, 0);

        when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(savedItem));
        when(itemMapper.toDto(savedItem, new HashMap<>())).thenReturn(expectedItemDto);

        StepVerifier.create(itemService.createItem(givenDto))
                .expectNext(expectedItemDto)
                .verifyComplete();

        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void createItem_shouldThrowTitleAlreadyExistsException_whenTitleAlreadyExistsFail() {
        ItemCreateDto givenDto = new ItemCreateDto("already-title", "description", 1000L);

        when(itemRepository.save(any(Item.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("Duplicate entry")));

        StepVerifier.create(itemService.createItem(givenDto))
                .expectError(TitleAlreadyExistsException.class)
                .verify();

        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void getItemByIdWithCart_shouldReturnDtoWithIdSuccess() {
        String imageUri = "data:image/jpeg;base64,test";
        ItemDto itemDtoWithoutImage = new ItemDto(VALID_ID, "Test Item", "Description", "", 1000L, 1);

        when(itemCacheService.getItemWithCache(VALID_ID)).thenReturn(Mono.just(testItemCache));
        when(itemCacheService.getImageFromCache("testImagePath.jpg")).thenReturn(Mono.just(imageUri));
        when(itemMapper.toDtoFromItemCacheWithImage(testItemCache, cartItemsCount, imageUri))
                .thenReturn(testItemDto);

        when(itemMapper.toDtoFromItemCacheWithoutImage(testItemCache, cartItemsCount))
                .thenReturn(itemDtoWithoutImage);

        StepVerifier.create(itemService.getItemByIdWithCartCount(VALID_ID, cartItemsCount))
                .expectNext(testItemDto)
                .verifyComplete();

        verify(itemCacheService, times(1)).getItemWithCache(VALID_ID);
        verify(itemCacheService, times(1)).getImageFromCache("testImagePath.jpg");
    }

    @Test
    void getItemByIdWithCartCount_shouldLoadImageFromServiceWhenNotInCache() {
        String imageUri = "data:image/jpg;base64,test";
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        ItemDto itemDtoWithoutImage = new ItemDto(VALID_ID, "Test Item", "Description", "", 1000L, 1);

        when(itemCacheService.getItemWithCache(VALID_ID)).thenReturn(Mono.just(testItemCache));
        when(itemCacheService.getImageFromCache("testImagePath.jpg")).thenReturn(Mono.empty());
        when(imageService.getImageByImgPath("testImagePath.jpg")).thenReturn(Mono.just(imageBytes));
        when(itemCacheService.saveImageToCache(eq("testImagePath.jpg"), anyString()))
                .thenReturn(Mono.empty());
        when(itemMapper.toDtoFromItemCacheWithImage(eq(testItemCache), eq(cartItemsCount), anyString()))
                .thenReturn(testItemDto);
        when(itemMapper.toDtoFromItemCacheWithoutImage(testItemCache, cartItemsCount))
                .thenReturn(itemDtoWithoutImage);

        StepVerifier.create(itemService.getItemByIdWithCartCount(VALID_ID, cartItemsCount))
                .expectNext(testItemDto)
                .verifyComplete();

        verify(itemCacheService).getItemWithCache(VALID_ID);
        verify(itemCacheService).getImageFromCache("testImagePath.jpg");
        verify(imageService).getImageByImgPath("testImagePath.jpg");
        verify(itemCacheService).saveImageToCache(eq("testImagePath.jpg"), anyString());
    }

    @Test
    void getItemByIdWithCartCount_shouldReturnItemWithoutImageWhenImageNotInCacheAndServiceFails() {
        // Arrange
        ItemDto itemDtoWithoutImage = new ItemDto(VALID_ID, "Test Item", "Description", "", 1000L, 1);

        when(itemCacheService.getItemWithCache(VALID_ID)).thenReturn(Mono.just(testItemCache));
        when(itemCacheService.getImageFromCache("testImagePath.jpg")).thenReturn(Mono.empty());
        when(imageService.getImageByImgPath("testImagePath.jpg")).thenReturn(Mono.empty());
        when(itemMapper.toDtoFromItemCacheWithoutImage(testItemCache, cartItemsCount))
                .thenReturn(itemDtoWithoutImage);

        StepVerifier.create(itemService.getItemByIdWithCartCount(VALID_ID, cartItemsCount))
                .expectNext(itemDtoWithoutImage)
                .verifyComplete();

        verify(itemCacheService).getItemWithCache(VALID_ID);
        verify(itemCacheService).getImageFromCache("testImagePath.jpg");
        verify(imageService).getImageByImgPath("testImagePath.jpg");
    }

    @Test
    void getItemByIdWithCartCount_shouldThrowItemNotFoundException_whenItemNotFoundFail() {
        // Arrange
        when(itemCacheService.getItemWithCache(INVALID_ID)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItemByIdWithCartCount(INVALID_ID, cartItemsCount))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(itemCacheService).getItemWithCache(INVALID_ID);
    }

    @Test
    void changeCartItemCount_shouldCallCartServiceMethodSuccess() {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        Map<Long, Integer> updatedCart = new HashMap<>(cartItemsCount);
        updatedCart.put(VALID_ID, 2);

        String imageUri = "data:image/jpeg;base64,test";
        ItemDto updatedItemDto = new ItemDto(VALID_ID, "Test Item", "Description", imageUri, 1000L, 2);
        ItemDto itemDtoWithoutImage = new ItemDto(VALID_ID, "Test Item", "Description", "", 1000L, 2);

        when(cartService.changeItemCount(givenDto)).thenReturn(Mono.just(2));
        when(itemCacheService.getItemWithCache(VALID_ID)).thenReturn(Mono.just(testItemCache));
        when(itemCacheService.getImageFromCache("testImagePath.jpg")).thenReturn(Mono.just(imageUri));
        when(itemMapper.toDtoFromItemCacheWithImage(testItemCache, updatedCart, imageUri))
                .thenReturn(updatedItemDto);

        when(itemMapper.toDtoFromItemCacheWithoutImage(testItemCache, updatedCart))
                .thenReturn(itemDtoWithoutImage);

        StepVerifier.create(itemService.changeCartItemCount(givenDto))
                .expectNext(updatedItemDto)
                .verifyComplete();

        verify(cartService).changeItemCount(givenDto);
        verify(itemCacheService).getItemWithCache(VALID_ID);
        verify(itemCacheService).getImageFromCache("testImagePath.jpg");
    }

    @Test
    void changeCartItemCount_shouldReturnItemWithoutImageWhenNoImage() {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        Map<Long, Integer> updatedCart = new HashMap<>(cartItemsCount);
        updatedCart.put(VALID_ID, 2);

        ItemCache itemCacheWithoutImage = new ItemCache(VALID_ID, "Test Item", "Description", 1000L, null);
        ItemDto updatedItemDto = new ItemDto(VALID_ID, "Test Item", "Description", "", 1000L, 2);

        when(cartService.changeItemCount(givenDto)).thenReturn(Mono.just(2));
        when(itemCacheService.getItemWithCache(VALID_ID)).thenReturn(Mono.just(itemCacheWithoutImage));
        when(itemMapper.toDtoFromItemCacheWithoutImage(itemCacheWithoutImage, updatedCart))
                .thenReturn(updatedItemDto);

        StepVerifier.create(itemService.changeCartItemCount(givenDto))
                .expectNext(updatedItemDto)
                .verifyComplete();

        verify(cartService).changeItemCount(givenDto);
        verify(itemCacheService).getItemWithCache(VALID_ID);
        verify(itemCacheService, never()).getImageFromCache(anyString());
    }

    @Test
    void getItemsPage_shouldHandleEmptyPage() {
        SearchDto givenDto = new SearchDto("nonexistent", SortColumn.PRICE, 1, 3, new HashMap<>());
        PageInfoCache emptyPageInfo = new PageInfoCache(
                Collections.emptyList(),
                3,
                1,
                false,
                false
        );

        when(itemCacheService.getPageWithCache(eq(givenDto), any(Pageable.class)))
                .thenReturn(Mono.just(emptyPageInfo));
        when(itemCacheService.getItemsWithCache(Collections.emptySet()))
                .thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItemsPage(givenDto))
                .expectNextMatches(pageDto -> {
                    assertThat(pageDto).isNotNull();
                    assertThat(pageDto.items()).isNotNull();
                    assertThat(pageDto.items()).isEmpty();
                    assertThat(pageDto.pageDto().pageNumber()).isEqualTo(1);
                    assertThat(pageDto.pageDto().pageSize()).isEqualTo(3);
                    assertThat(pageDto.pageDto().hasPrevious()).isFalse();
                    assertThat(pageDto.pageDto().hasNext()).isFalse();
                    return true;
                })
                .verifyComplete();
    }
}