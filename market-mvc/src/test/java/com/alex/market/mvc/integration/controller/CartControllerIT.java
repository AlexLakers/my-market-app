package com.alex.market.mvc.integration.controller;

import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.model.CartAction;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.With;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@EnableWireMock(@ConfigureWireMock(name = "payment-service", port = 0))
@TestPropertySource(properties = {"market.upload.payment-service-url=http://localhost:${wiremock.server.port}"})
@WithMockUser(username = "tets",password = "test", authorities = "USER")
class CartControllerIT extends BaseIntegrationTest {

    @MockitoBean(reset = MockReset.BEFORE)
    private CartWebFilter cartWebFilter;

    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1000L;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);

        mockCartWebFilter();
    }

    @InjectWireMock("payment-service")
    private WireMockServer mockPaymentService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void changeCartItemCountForCartPage_shouldRedirectToGetItemsSuccess() {

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

    @ParameterizedTest
    @CsvSource({
            "20000, SUCCESS, Купить",
            "10, FAILED, Недостаточно средств. Пополните баланс."
    })
    void getItemsCartWithBalanceStatus_shouldCheckBalanceAndReturn(int balance, String status, String htmlSpecific) {
        String failedResponse = String.format("{\"accountId\":1,\"balance\":%1$d,\"status\":\"%1%s\"}", balance, status);

        mockPaymentService.stubFor(get("/api/payments/accounts/1")
                .willReturn(okJson(failedResponse)));

        webTestClient

                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("<title>Корзина</title>");
                    assert html.contains("<input type=\"hidden\" name=\"id\" value=\"1000\">");
                    assert html.contains("<h2>Итого: 1000 руб.</h2>");
                    assert html.contains(htmlSpecific);
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