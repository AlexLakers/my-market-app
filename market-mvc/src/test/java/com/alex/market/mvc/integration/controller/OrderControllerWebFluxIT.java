package com.alex.market.mvc.integration.controller;

import com.alex.market.mvc.config.ConfigProperties;
import com.alex.market.mvc.controller.OrderController;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.dto.output.OrderDto;
import com.alex.market.mvc.dto.output.OrderPaymentDto;
import com.alex.market.mvc.exception.OrderNotFoundException;
import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.service.UserService;
import com.alex.market.mvc.service.OrderService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@WebFluxTest(OrderController.class)
@Import(ConfigProperties.class)
@ActiveProfiles("test")
@WithMockUser(username = "test@yandex.ru",password = "test", authorities = "USER")
class OrderControllerWebFluxIT {
    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @MockitoBean(reset = MockReset.BEFORE)
    private CartWebFilter cartWebFilter;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        mockCartWebFilter();
    }

    @Autowired
    private WebTestClient testClient;

    @MockitoBean(reset = MockReset.BEFORE)
    private OrderService orderService;

    @MockitoBean(reset = MockReset.BEFORE)
    private UserService userService;

      @Test
    void shouldHaveCorrectPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       Assertions.assertThat("test@yandex.ru").isEqualTo(auth.getName());
          Assertions.assertThat(auth.getAuthorities().stream()
                  .anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void getAllOrders_shouldSet200AndReturnOrdersForUserPageWithData() {
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto), 1000L);
        when(orderService.findAllPaidOrdersForAuthUser()).thenReturn(Flux.fromIterable(List.of(orderDto)));

        testClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("testTitle1 (1 шт.) 1000 руб.");
                });
    }

    @Test
    void getOrderById_shouldReturnOneDtoAndViewSuccess() {
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto), 1000L);
        when(orderService.findOrderWithItems(VALID_ID)).thenReturn(Mono.just(orderDto));

        testClient.get()
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
    void getOrderById__shouldSetStatus404_whenNotFoundFail() {
        when(orderService.findOrderWithItems(INVALID_ID)).thenReturn(Mono.error(new OrderNotFoundException(INVALID_ID)));
        testClient.get()
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
    void createOrderForAuthUser_shouldSet201AndRedirectToOrderForUserPage() {
        OrderPaymentDto orderPaymentDto = new OrderPaymentDto(VALID_ID,"PAID");
        when(orderService.createAndProcessOrderForAuthUser(anyMap())).thenReturn(Mono.just(orderPaymentDto));

        testClient.mutateWith(SecurityMockServerConfigurers.csrf()).post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()

                .expectHeader().location("/orders/" + VALID_ID + "?newOrder=true")
                .expectBody(String.class);

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