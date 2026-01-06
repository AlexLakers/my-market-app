package com.alex.market.controller;

import com.alex.market.config.ConfigProperties;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.service.ItemService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(OrderController.class)
@Import(ConfigProperties.class)
@ActiveProfiles("test")
class OrderControllerTest {
    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @Autowired
    private WebTestClient testClient;

    @MockitoBean(reset = MockReset.BEFORE)
    private OrderService orderService;

    @Test
    void getAllOrders() {
       ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        Mockito.when(orderService.findAllOrders()).thenReturn(Flux.fromIterable(List.of(orderDto)));

        testClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html->{
                    assert html.contains("testTitle1 (1 шт.) 1000 руб.");
                });

    }
}