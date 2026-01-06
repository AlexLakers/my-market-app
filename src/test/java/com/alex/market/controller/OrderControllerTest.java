package com.alex.market.controller;

import com.alex.market.config.ConfigProperties;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.exception.OrderNotFoundException;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.model.Order;
import com.alex.market.repository.projection.OrderItemsDetails;
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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

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
    void getAllOrders_shouldSet200AndReturnOrdersPageWithData() {
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
    @Test
    void getOrderById_shouldReturnOneDtoAndViewSuccess(){
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        Mockito.when(orderService.findOrderWithItems(VALID_ID)).thenReturn(Mono.just(orderDto));

        testClient.get()
                .uri("/orders/" + VALID_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html->{
                    assert html.contains("Заказ №1");
                });
    }

    @Test
    void getOrderById_shouldSetStatus404_whenNotFoundFail(){
        Mockito.when(orderService.findOrderWithItems(INVALID_ID)).thenReturn(Mono.error(new OrderNotFoundException(INVALID_ID)));

        testClient.get()
                .uri("/orders/" + INVALID_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html->{
                    assert html.contains("404");
                });
    }

}