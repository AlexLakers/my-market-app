package com.alex.market.controller;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.filter.CartWebFilter;
import com.alex.market.model.Item;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.MockServerConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(ItemController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ItemControllerTest {

    @Autowired
    private WebTestClient testClient;

    @MockitoBean(reset = MockReset.BEFORE)
    private ItemService itemService;

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

        Mockito.when(itemService.getItemsPage(Mockito.any(SearchDto.class))).thenReturn(Mono.just(expectedDto));

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

        Mockito.when(itemService.getItemsPage(Mockito.any(SearchDto.class))).thenReturn(Mono.just(expectedDto));

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

    void mockCartWebFilter() {
        Mockito.when(cartWebFilter.filter(Mockito.any(ServerWebExchange.class), Mockito.any(WebFilterChain.class)))
                .thenAnswer(invocation -> {
                    ServerWebExchange exchange = invocation.getArgument(0);
                    WebFilterChain chain = invocation.getArgument(1);
                    return exchange.getSession()
                            .doOnNext(session -> session.getAttributes().put("cart", cartItemsCount))
                            .then(chain.filter(exchange));
                });
    }
}