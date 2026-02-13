package com.alex.market.payment.rest.controller;

import com.alex.market.payment.api.DefaultApi;
import com.alex.market.payment.api.dto.AccountResponse;
import com.alex.market.payment.api.dto.PaymentRequest;
import com.alex.market.payment.api.dto.PaymentResponse;
import com.alex.market.payment.service.AccountService;
import com.alex.market.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.print.attribute.standard.Media;

@RestController
@RequiredArgsConstructor
public class PaymentApiController implements DefaultApi {

    private final AccountService accountService;
    private final PaymentService paymentService;

    @Override
    @PreAuthorize("hasAuthority('PAYMENT-ACCESS')")
    public Mono<ResponseEntity<AccountResponse>> getAccountById(
            @PathVariable("accountId") Long accountId,
            ServerWebExchange exchange
    ) {
        if (accountId == null) return Mono.just(ResponseEntity.badRequest().build());

        return accountService.getAccountById(accountId)
                .map(accResp -> ResponseEntity
                        .status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(accResp));
    }

    @Override
    @PreAuthorize("hasAuthority('PAYMENT-ACCESS')")
    public Mono<ResponseEntity<PaymentResponse>> processPayment(
            @Valid @RequestBody Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange
    ) {

        return paymentRequest.flatMap(paymentService::processPaymentInTransaction)
                .map(payResp -> ResponseEntity
                        .status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payResp));
    }
}
