package com.alex.market.payment.service;

import com.alex.market.payment.api.dto.PaymentRequest;
import com.alex.market.payment.api.dto.PaymentResponse;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<PaymentResponse> processPaymentInTransaction(PaymentRequest paymentRequest);
}
