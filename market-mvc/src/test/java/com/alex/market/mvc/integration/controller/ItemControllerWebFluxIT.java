package com.alex.market.mvc.integration.controller;

import com.alex.market.mvc.config.ConfigProperties;
import com.alex.market.mvc.controller.ItemController;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.input.ItemCreateDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.PageDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.exception.TitleAlreadyExistsException;
import com.alex.market.mvc.exception.handler.GlobalExceptionHandler;
import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.search.PageItemsDto;
import com.alex.market.mvc.search.SearchDto;
import com.alex.market.mvc.search.SortColumn;
import com.alex.market.mvc.service.ImageService;
import com.alex.market.mvc.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@WebFluxTest(ItemController.class)
@Import({ConfigProperties.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ItemControllerWebFluxIT {

    @Autowired
    private WebTestClient testClient;

    @MockitoBean(reset = MockReset.BEFORE)
    private ItemService itemService;

    @MockitoBean
    private ImageService imageService;
    @MockitoBean(reset = MockReset.BEFORE)
    private CartWebFilter cartWebFilter;

    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        cartItemsCount.put(2L, 3);

        mockCartWebFilter();
    }


    @Test
    void getItems_shouldSet200StatusAndReturnHtmlPageWithModel() {
        PageItemsDto expectedDto = new PageItemsDto(List.of(List.of(new ItemDto(VALID_ID, "test1-title", "test1-desc", "/img/path", 1000L, 3))), SortColumn.PRICE.name(), "test1", new PageDto(3, 1, false, false));

        when(itemService.getItemsPage(any(SearchDto.class))).thenReturn(Mono.just(expectedDto));

        testClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "test")
                        .queryParam("sort", SortColumn.NO.name())
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "3")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("<span class=\"badge text-bg-success\">Витрина магазина</span>");
                    assert html.contains("test1-title");
                });
    }

    @Test
    void getItems_shouldSet200StatusAndReturnHtmlPageWithModel_whenParamsNotGiven() {
        PageItemsDto expectedDto = new PageItemsDto(List.of(List.of(new ItemDto(VALID_ID, "test1-title", "test1-desc", "/img/path", 1000L, 3))), SortColumn.PRICE.name(), "test1", new PageDto(3, 1, false, false));

        when(itemService.getItemsPage(any(SearchDto.class))).thenReturn(Mono.just(expectedDto));

        testClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("<span class=\"badge text-bg-success\">Витрина магазина</span>");
                    assert html.contains("test1-title");
                });
    }

    @Test
    void createItem_shouldSet201StatusAndReturnHtmlPageNewImageSuccess() {
        ItemCreateDto givenDto = new ItemCreateDto("test-title", "description", 1000L);
        ItemDto itemDto = new ItemDto(VALID_ID, givenDto.title(), givenDto.description(), null, givenDto.price(), 1);
        when(itemService.createItem(givenDto)).thenReturn(Mono.just(itemDto));

        testClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/new")
                        .queryParam("title", givenDto.title())
                        .queryParam("description", givenDto.description())
                        .queryParam("price", givenDto.price())
                        .build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("<i class=\"bi bi-check-circle\"></i> Товар успешно создан!");
                    assert html.contains("test-title");
                });
    }

    @Test
    void createItem_shouldSet400StatusAndReturnHtmlPage400Fail() {
        ItemCreateDto givenDto = new ItemCreateDto("already-title", "description", 1000L);
        doThrow(TitleAlreadyExistsException.class).when(itemService).createItem(givenDto);

        testClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/new")
                        .queryParam("title", givenDto.title())
                        .queryParam("description", givenDto.description())
                        .queryParam("price", givenDto.price())
                        .build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Неверный запрос:");
                });
    }

    @Test
    void updateImageById_shouldUpdateImageByItemIdSuccess() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new ByteArrayResource("image/jpeg".getBytes()))
                .filename("image.jpg")
                .contentType(MediaType.IMAGE_JPEG);


        when(imageService.updateImageByItemId(any(FilePart.class), anyLong())).thenReturn(Mono.empty());

        testClient.post()
                .uri("/items/{id}/images/new", VALID_ID)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items/" + VALID_ID)
                .expectBody(String.class);
    }

    @Test
    void updateImageById_shouldSet404StatusAndReturnErrorPage_whenItemNotFountFail() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new ByteArrayResource("image/jpeg".getBytes()))
                .filename("image.jpg")
                .contentType(MediaType.IMAGE_JPEG);


        when(imageService.updateImageByItemId(any(FilePart.class), anyLong()))
                .thenReturn(Mono.error(() -> new ItemNotFoundException(INVALID_ID)));

        testClient.post()
                .uri("/items/{id}/images/new", INVALID_ID)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Страница не найдена");
                });
    }

    @Test
    void getItemByIdWithCartCount_shouldSet200AndReturnItemByIdSuccess() {
        ItemDto itemDto = new ItemDto(VALID_ID, "title", "description", null, 1000L, 1);
        when(itemService.getItemByIdWithCartCount(VALID_ID, cartItemsCount)).thenReturn(Mono.just(itemDto));

        testClient.get()
                .uri("/items/{id}", VALID_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("title");
                    assert html.contains("description");
                });
    }

    @Test
    void getItemByIdWithCartCount_shouldSet404AndReturnErrorPageFail() {
        when(itemService.getItemByIdWithCartCount(INVALID_ID, cartItemsCount)).thenReturn(Mono.error(new ItemNotFoundException(INVALID_ID)));

        testClient.get()
                .uri("/items/{id}", INVALID_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("404");
                });
    }

    @Test
    void changeCartItemCountForItemsPage_shouldRedirectItemsPageWithAttrs() {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID) + 1);
        when(itemService.changeCartItemCount(givenDto)).thenReturn(Mono.just(expectedDto));
        testClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", givenDto.itemId())
                        .queryParam("action", givenDto.action())
                        .queryParam("search", "test")
                        .queryParam("sort", SortColumn.NO.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=test&sort=NO&pageSize=3&pageNumber=1")
                .expectBody(String.class);
    }

    @Test
    void changeCartItemCountForItemPage_shouldSet200AndReturnItemPageWithModel() {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID) + 1);
        when(itemService.changeCartItemCount(givenDto)).thenReturn(Mono.just(expectedDto));

        testClient.post()
                .uri("/items/{itemId}?action=" + CartAction.PLUS.name(), VALID_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("testTitle1");
                    assert html.contains("testDesc1");
                    assert html.contains("<span>3</span>");
                });
    }

    void mockCartWebFilter() {
        when(cartWebFilter.filter(any(ServerWebExchange.class), any(WebFilterChain.class)))
                .thenAnswer(invocation -> {
                    ServerWebExchange exchange = invocation.getArgument(0);
                    WebFilterChain chain = invocation.getArgument(1);
                    return exchange.getSession()
                            .doOnNext(session -> session.getAttributes().put("cart", cartItemsCount))
                            .then(chain.filter(exchange));
                });
    }
}