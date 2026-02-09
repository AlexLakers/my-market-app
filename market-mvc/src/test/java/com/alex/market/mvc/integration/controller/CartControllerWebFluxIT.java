package com.alex.market.mvc.integration.controller;

import com.alex.market.mvc.config.ConfigProperties;
import com.alex.market.mvc.controller.CartController;
import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.output.AccountBalanceDto;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.PaymentApiStatus;
import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
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

@WebFluxTest(CartController.class)
@Import(ConfigProperties.class)
@WithMockUser(username = "test",password = "test", authorities = "USER")
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
        AccountBalanceDto accountBalanceDto=new AccountBalanceDto(VALID_ID,3000L, PaymentApiStatus.SUCCESS);
        CartDto expectedDto = new CartDto(List.of(itemDto), 2000L,PaymentApiStatus.SUCCESS.name());
        when(cartService.changeItemCount(givenDto)).thenReturn(Mono.just(itemDto.count()));
        when(cartService.getItemsCartWithBalanceStatus(cartItemsCount)).thenReturn(Mono.just(expectedDto));

        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
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