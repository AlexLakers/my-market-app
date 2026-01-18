package com.alex.market.payment.integration.controller;

import com.alex.market.payment.api.dto.AccountResponse;
import com.alex.market.payment.api.dto.PaymentRequest;
import com.alex.market.payment.api.dto.PaymentResponse;
import com.alex.market.payment.api.dto.TransactionStatus;
import com.alex.market.payment.exception.AccountNotFoundException;
import com.alex.market.payment.exception.ErrorResponse;
import com.alex.market.payment.exception.handler.GlobalExceptionHandler;
import com.alex.market.payment.integration.BaseIntegrationTest;
import com.alex.market.payment.rest.controller.PaymentApiController;
import com.alex.market.payment.service.AccountService;
import com.alex.market.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentApiControllerIT extends BaseIntegrationTest {

    private final static Long VALID_ID = 1L;
    private final static Long INVALID_ID = -1L;

    @Autowired
    private WebTestClient webClient;

    @Test
    void getAccountById_shouldReturnAccountResponseJson() {
        AccountResponse accountResponse = new AccountResponse(1L, 500000L);

        webClient.get()
                .uri("/api/payments/accounts/{accountId}", VALID_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(AccountResponse.class)
                .value(json -> assertEquals(accountResponse, json));
    }

    @Test
    void getAccountById_shouldSet404StatusAndErrorMessageBody() {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "The account with id: -1 is not found");

        webClient.get()
                .uri("/api/payments/accounts/{accountId}", INVALID_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(ErrorResponse.class)
                .value(json -> assertEquals(error, json));
    }

    @Test
    void processPayment_shouldSet200StatusAndReturnPaymentResponseJson() {
        PaymentRequest request = new PaymentRequest(VALID_ID, VALID_ID,VALID_ID, 10000L);
        PaymentResponse response = new PaymentResponse(VALID_ID, VALID_ID, VALID_ID, TransactionStatus.SUCCESS, 10000L);

        webClient.post()
                .uri("/api/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), PaymentRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(PaymentResponse.class)
                .value(json -> assertEquals(response, json));
    }
}