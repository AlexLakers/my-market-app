package com.alex.market.integration.controller;

import com.alex.market.config.ConfigProperties;
import com.alex.market.controller.CartController;
import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.output.CartDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.filter.CartWebFilter;
import com.alex.market.model.CartAction;
import com.alex.market.service.CartService;
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

@WebFluxTest(CartController.class)
@Import(ConfigProperties.class)
class CartControllerWebFluxIT {
    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1L;

    @MockitoBean(reset = MockReset.BEFORE)
    private CartWebFilter cartWebFilter;
    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        mockCartWebFilter();
    }

    @MockitoBean(reset = MockReset.BEFORE)
    private CartService cartService;

    @Autowired
    private WebTestClient webTestClient;
    @Test
    void changeCartItemCountForCartPage_shouldRedirectToGetItemsSuccess() {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID) + 1);
        CartDto expectedDto = new CartDto(List.of(itemDto), 2000L);
        Mockito.when(cartService.changeItemCount(givenDto)).thenReturn(Mono.just(itemDto.count()));
        Mockito.when(cartService.getItemsCartWithTotal(cartItemsCount)).thenReturn(Mono.just(expectedDto));

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
  /*  @Test
    void changeCartItemCountForCartPage_shouldRedirectToGetItems() throws Exception {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID) + 1);
        CartDto expectedDto = new CartDto(List.of(itemDto), 2000L);
        when(cartService.changeItemCount(givenDto)).thenReturn(itemDto.count());
        when(cartService.getItemsCartWithTotal(cartItemsCount)).thenReturn(expectedDto);

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(VALID_ID))
                        .param("action", CartAction.PLUS.name())
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

    }
}*/
}