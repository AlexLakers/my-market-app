package com.alex.market.mvc.integration.controller;


import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.input.ItemCreateDto;
import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.search.SortColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ItemControllerIT extends BaseIntegrationTest {

    @Autowired
    private WebTestClient testClient;

    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1000L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @MockitoBean(reset = MockReset.BEFORE)
    private CartWebFilter cartWebFilter;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        cartItemsCount.put(2L, 3);
        mockCartWebFilter();
    }


    @Test
    @WithAnonymousUser
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
    @WithAnonymousUser
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
    @WithMockUser(username = "test@yandex.ru",password = "password",authorities = "USER")
    void createItem_shouldSet201StatusAndReturnHtmlPageNewImageSuccess() {
        ItemCreateDto givenDto = new ItemCreateDto("test-title", "description", 1000L);

        testClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
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
    @WithMockUser(authorities = "USER")
    void createItem_shouldSet400StatusAndReturnHtmlPage400Fail() {
        ItemCreateDto givenDto = new ItemCreateDto("test1-ball", "description", 1000L);

        testClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
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
    @WithMockUser(authorities = "USER")
    void updateImageById_shouldUpdateImageByItemIdSuccess() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new ByteArrayResource("image/jpeg".getBytes()))
                .filename("image.jpg")
                .contentType(MediaType.IMAGE_JPEG);

        testClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
                .uri("/items/{id}/images/new", VALID_ID)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items/" + VALID_ID)
                .expectBody(String.class);
    }

    @Test
    @WithMockUser(authorities = "USER")
    void updateImageById_shouldSet404StatusAndReturnErrorPage_whenItemNotFountFail() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new ByteArrayResource("image/jpeg".getBytes()))
                .filename("image.jpg")
                .contentType(MediaType.IMAGE_JPEG);

        testClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
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
    @WithAnonymousUser
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
    @WithAnonymousUser
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
    @WithMockUser(authorities = "USER")
    void changeCartItemCountForItemsPage_shouldRedirectItemsPageWithAttrs() {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);

        testClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
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
    @WithMockUser(authorities = "USER")
    void changeCartItemCountForItemPage_shouldSet200AndReturnItemPageWithModel() {
        testClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
                .uri("/items/{itemId}?action=" + CartAction.PLUS.name(), VALID_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("test1-ball");
                    assert html.contains("Test ball description1");
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