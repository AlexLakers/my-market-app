package com.alex.market.mvc.service;

import com.alex.market.mvc.client.dto.PaymentRequest;
import com.alex.market.mvc.dto.output.AccountBalanceDto;
import com.alex.market.mvc.dto.output.PaymentDto;
import reactor.core.publisher.Mono;

public interface PaymentApiClientService {
    Mono<PaymentDto> processPaymentInTransaction(PaymentRequest paymentRequest);
    Mono<AccountBalanceDto> getAccountById(Long accountId);
}
