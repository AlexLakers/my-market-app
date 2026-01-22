package com.alex.market.mvc.service.impl;

import com.alex.market.mvc.client.api.DefaultApi;
import com.alex.market.mvc.client.dto.AccountResponse;
import com.alex.market.mvc.client.dto.PaymentRequest;
import com.alex.market.mvc.client.dto.PaymentResponse;
import com.alex.market.mvc.client.dto.TransactionStatus;
import com.alex.market.mvc.dto.output.AccountBalanceDto;
import com.alex.market.mvc.dto.output.PaymentApiStatus;
import com.alex.market.mvc.dto.output.PaymentDto;
import com.alex.market.mvc.service.PaymentApiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApiClientServiceImpl implements PaymentApiClientService {

    private final DefaultApi paymentApi;
    private static final Duration PAYMENT_TIMEOUT = Duration.ofSeconds(5);


    @Override
    public Mono<PaymentDto> processPaymentInTransaction(PaymentRequest paymentRequest) {
        Long orderId = paymentRequest.getOrderId();
        return paymentApi.processPayment(paymentRequest)
                .timeout(PAYMENT_TIMEOUT)
                .map(response -> {
                    PaymentApiStatus paymentApiStatus =
                            response.getStatus() == TransactionStatus.SUCCESS
                                    ? PaymentApiStatus.SUCCESS
                                    : PaymentApiStatus.BUSINESS_FAIL;
                    log.info("Payment processing status: {}", paymentApiStatus);
                    return createPaymentDto(paymentApiStatus, response);
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Payment service error, HTTP-status: {}, body: {}",
                            e.getStatusCode().value(), e.getResponseBodyAsString());

                    return Mono.just(createErrorPaymentDto(orderId, PaymentApiStatus.SERVICE_ERROR));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Payment service is unavailable: {}", e.getMessage());

                    return Mono.just(createErrorPaymentDto(orderId, PaymentApiStatus.NETWORK_ERROR));
                });

    }

    private PaymentDto createPaymentDto(PaymentApiStatus status, PaymentResponse paymentResponse) {
        return new PaymentDto(status, paymentResponse.getOrderId(), paymentResponse.getTransactionId(), paymentResponse.getFailureReason());
    }

    private PaymentDto createErrorPaymentDto(Long orderId, PaymentApiStatus status) {
        return new PaymentDto(status, orderId, null, "");
    }




}


