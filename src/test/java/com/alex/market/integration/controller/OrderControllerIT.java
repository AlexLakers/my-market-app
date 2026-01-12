package com.alex.market.integration.controller;

import com.alex.market.config.ConfigProperties;
import com.alex.market.controller.OrderController;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.exception.OrderNotFoundException;
import com.alex.market.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


class OrderControllerIT extends BaseIntegrationTest{
    private static final Long VALID_ID = 1000L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @Autowired
    private WebTestClient testClient;

    @Autowired
    private OrderService orderService;

    @Test
    void getAllOrders_shouldSet200AndReturnOrdersPageWithData() {
        testClient.get()
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
    void getOrderById_shouldSetStatus404_whenNotFoundFail() {
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
    void createOrder_shouldSet201AndRedirectToOrderPage() {
        final long newSavedId=1L;
        testClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/orders/" + newSavedId + "?newOrder=true")
                .expectBody(String.class);

        testClient.get()
                .uri("/orders/{id}", newSavedId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Заказ №"+newSavedId);
                });
    }
}