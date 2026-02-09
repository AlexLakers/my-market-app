package com.alex.market.mvc.integration.controller;

import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.security.model.CustomUserDetails;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@EnableWireMock(@ConfigureWireMock(name = "payment-service", port = 0))
@TestPropertySource(properties = {"market.upload.payment-service-url=http://localhost:${wiremock.server.port}"})
/*@WithUserDetails(
        value = "lakers@yandex.ru",
        setupBefore = TestExecutionEvent.TEST_EXECUTION
)*/
class OrderControllerIT extends BaseIntegrationTest {
    private static final Long VALID_ID = 1000L;
    private static final Long INVALID_ID = Long.MAX_VALUE;


    @MockitoBean(reset = MockReset.BEFORE)
    private CartWebFilter cartWebFilter;

    private Map<Long, Integer> cartItemsCount;


    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);

        mockCartWebFilter();
    }

    @InjectWireMock("payment-service")
    private WireMockServer mockPaymentService;

    @Autowired
    private WebTestClient testClient;

   /* @Test
    void shouldHaveCorrectPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("lakers@yandex.ru", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
    }*/

    @Test
    void getAllOrders_shouldSet200AndReturnOrdersPageWithData() {

        testClient
                .mutateWith(SecurityMockServerConfigurers.mockUser("testUser").password("testPassword").authorities("USER"))
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("test1-ball (2 шт.) 1000 руб.");
                });
    }

    @Test
    void getOrderById_shouldReturnOneDtoAndViewSuccess() {
        testClient.mutateWith(SecurityMockServerConfigurers.mockUser("testUser").password("testPassword").authorities("USER"))
                .get()
                .uri("/orders/" + VALID_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Заказ №1");
                });
    }

    @Test
    void getOrderById_shouldSetStatus404_whenNotFoundFail() {
        testClient.mutateWith(SecurityMockServerConfigurers.mockUser("testUser").password("testPassword").authorities("USER"))
                .get()
                .uri("/orders/" + INVALID_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("404");
                });
    }


    @Test
    void createAndProcessOrder_shouldRedirectWithFailedStatus_Failed() {

        CustomUserDetails userDetails = new CustomUserDetails(
                "lakers@yandex.ru",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("USER")),
                1L
        );

        String failedResponse = "{\"accountId\":1,\"orderId\":1,\"transactionId\":31,\"status\":\"FAILED\",\"failureReason\":\"Amount must be positive and account with id: 1\",\"amount\":1000}";

        mockPaymentService.stubFor(post("/api/payments/pay")
                .willReturn(okJson(failedResponse)));

        testClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .mutateWith(SecurityMockServerConfigurers.mockUser(userDetails))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items?paymentOrderStatus=FAILED");
    }

    @Test
    void createAndProcessOrder_shouldCreateAndProcessOrderRedirectToNewOrderPage_Success() {

        CustomUserDetails userDetails = new CustomUserDetails(
                "lakers@yandex.ru",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("USER")),
                1L
        );

        final long newSavedId = 1L;
        mockPaymentService.stubFor(post("/api/payments/pay")
                .willReturn(okJson("{\"accountId\":1,\"orderId\":1,\"transactionId\":30,\"status\":\"SUCCESS\",\"amount\":1000}")));

        testClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .mutateWith(SecurityMockServerConfigurers.mockUser(userDetails))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/orders/" + newSavedId + "?newOrder=true")
                .expectBody(String.class);

        testClient.mutateWith(SecurityMockServerConfigurers.mockUser()
                        .authorities("USER"))
                .get()
                .uri("/orders/{id}", newSavedId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Заказ №" + newSavedId);
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