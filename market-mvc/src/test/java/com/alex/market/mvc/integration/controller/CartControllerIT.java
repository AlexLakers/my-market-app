package com.alex.market.mvc.integration.controller;

import com.alex.market.mvc.config.ConfigProperties;
import com.alex.market.mvc.controller.CartController;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


class CartControllerIT extends BaseIntegrationTest {
    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1000L;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
    }


    @Autowired
    private WebTestClient webTestClient;

    @Test
    void changeCartItemCountForCartPage_shouldRedirectToGetItemsSuccess() {

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", String.valueOf(VALID_ID))
                        .queryParam("action", CartAction.PLUS.name())
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items");
    }
}