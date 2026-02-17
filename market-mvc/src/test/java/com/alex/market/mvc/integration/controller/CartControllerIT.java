package com.alex.market.mvc.integration.controller;

import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.security.model.CustomUserDetails;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.With;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@EnableWireMock(@ConfigureWireMock(name = "payment-service", port = 0))
@TestPropertySource(properties = {"market.upload.payment-service-url=http://localhost:${wiremock.server.port}"})
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
    @WithMockUser(username = "test",password = "test", authorities = "USER")
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

    @Test
    void changeCartItemCountForCartPage_shouldRedirectToLoginPage_whenUserIsNotAuthorized() {

        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", String.valueOf(VALID_ID))
                        .queryParam("action", CartAction.PLUS.name())
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login");
    }

    @ParameterizedTest
    @CsvSource({
            "20000, SUCCESS, Купить",
            "10, FAILED, Недостаточно средств. Пополните баланс."
    })
    void getItemsCartWithBalanceStatus_shouldCheckBalanceAndReturn(int balance, String status, String htmlSpecific) {
        String failedResponse = String.format("{\"accountId\":1,\"balance\":%1$d,\"status\":\"%1%s\"}", balance, status);

        CustomUserDetails userDetails = new CustomUserDetails(
                "test@yandex.ru",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("USER")),
                1L
        );

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "fake-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Set.of("PAYMENT-ACCESS", "write")
        );

        mockPaymentService.stubFor(get("/api/payments/accounts/1")
                .willReturn(okJson(failedResponse)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .mutateWith(SecurityMockServerConfigurers.mockUser(userDetails))
                .mutateWith(SecurityMockServerConfigurers.mockOAuth2Client()
                        .clientRegistration(ClientRegistration.withRegistrationId("keycloak-test")
                                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                .clientId("market-mvc-test")
                                .tokenUri("keycloak-test/token")
                                .build())
                        .principalName("market-mvc-test")
                        .accessToken(accessToken))
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

    @Test
    void getItemsCartWithBalanceStatus_shouldSet404_whenAuthoritiesIsNotEnough() {
        String failedResponse = String.format("{\"accountId\":1,\"balance\":%1$d,\"status\":\"%1%s\"}", 1000, "STATUS");

        CustomUserDetails NotValidUserDetails = new CustomUserDetails(
                "test@yandex.ru",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("NOT-VALID-ROLE")),
                1L
        );


        mockPaymentService.stubFor(get("/api/payments/accounts/1")
                .willReturn(okJson(failedResponse)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .mutateWith(SecurityMockServerConfigurers.mockUser(NotValidUserDetails))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isForbidden();
    }
    @Test
    void getItemsCartWithBalanceStatus_shouldRedirectToLoginPage_whenUserIsNotAuthorized() {
        String failedResponse = String.format("{\"accountId\":1,\"balance\":%1$d,\"status\":\"%1%s\"}", 1000, "STATUS");

        mockPaymentService.stubFor(get("/api/payments/accounts/1")
                .willReturn(okJson(failedResponse)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection().expectHeader().location("/login");
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