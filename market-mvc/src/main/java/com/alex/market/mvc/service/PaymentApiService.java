package com.alex.market.mvc.service;

import com.alex.market.mvc.client.dto.PaymentRequest;
import com.alex.market.mvc.dto.output.PaymentResultDto;
import reactor.core.publisher.Mono;

public interface PaymentApiService {
    Mono<PaymentResultDto> processPaymentInTransaction(PaymentRequest paymentRequest);
}
