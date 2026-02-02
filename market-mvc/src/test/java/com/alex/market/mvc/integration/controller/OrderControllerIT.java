package com.alex.market.mvc.integration.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;

@EnableWireMock(@ConfigureWireMock(name = "payment-service", port = 0))
@TestPropertySource(properties = {"market.upload.payment-service-url=http://localhost:${wiremock.server.port}"})
class OrderControllerIT extends BaseIntegrationTest {
    private static final Long VALID_ID = 1000L;
    private static final Long INVALID_ID = Long.MAX_VALUE;


    @InjectWireMock("payment-service")
    private WireMockServer mockPaymentService;

    @Autowired
    private WebTestClient testClient;

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
    void createAndProcessOrder_shouldRedirectWithFailedStatus_Failed() {
        String failedResponse = "{\"accountId\":1,\"orderId\":1,\"transactionId\":31,\"status\":\"FAILED\",\"failureReason\":\"Amount must be positive and account with id: 1\",\"amount\":1000}";

        mockPaymentService.stubFor(post("/api/payments/pay")
                .willReturn(okJson(failedResponse)));

        testClient
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items?paymentOrderStatus=FAILED");
    }

    @Test
    void createAndProcessOrder_shouldCreateAndProcessOrderRedirectToNewOrderPage_Success() {
        final long newSavedId = 1L;
        mockPaymentService.stubFor(post("/api/payments/pay")
                .willReturn(okJson("{\"accountId\":1,\"orderId\":1,\"transactionId\":30,\"status\":\"SUCCESS\",\"amount\":1000}")));

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
                    assert html.contains("Заказ №" + newSavedId);
                });
    }


}