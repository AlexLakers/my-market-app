package com.alex.market.integration.controller;


import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.model.CartAction;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ImageService;
import com.alex.market.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.HashMap;
import java.util.Map;
class ItemControllerIT extends BaseIntegrationTest {

    @Autowired
    private WebTestClient testClient;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ImageService imageService;

    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1000L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        cartItemsCount.put(2L, 3);
    }


    @Test
    void getItems_shouldSet200StatusAndReturnHtmlPageWithModel() {

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
                    assert html.contains("test1-ball");
                });
    }

    @Test
    void getItems_shouldSet200StatusAndReturnHtmlPageWithModel_whenParamsNotGiven() {

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
                    assert html.contains("test1-ball");
                });
    }

    @Test
    void createItem_shouldSet201StatusAndReturnHtmlPageNewImageSuccess() {
        ItemCreateDto givenDto = new ItemCreateDto("test-title", "description", 1000L);

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
        ItemCreateDto givenDto = new ItemCreateDto("test1-ball", "description", 1000L);

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
        testClient.post()
                .uri("/items/{itemId}?action=" + CartAction.PLUS.name(), VALID_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("test1-ball");
                    assert html.contains("Test ball description1");
                    assert html.contains("<span>1</span>");
                });
    }
}