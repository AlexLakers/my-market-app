package com.alex.market.payment.rest.controller;

import com.alex.market.payment.api.DefaultApi;
import com.alex.market.payment.api.dto.AccountResponse;
import com.alex.market.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PaymentApiController implements DefaultApi {

    private final AccountService accountService;

    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccountById(
            @PathVariable("accountId") Long accountId,
            ServerWebExchange exchange
    ) {
        if (accountId == null) return Mono.just(ResponseEntity.badRequest().build());

        return accountService.getAccountById(accountId)
                .map(acc -> ResponseEntity
                        .status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(acc));
    }
}
