package com.alex.market.mvc.service.impl;

import com.alex.market.mvc.client.api.DefaultApi;
import com.alex.market.mvc.client.dto.PaymentRequest;
import com.alex.market.mvc.client.dto.PaymentResponse;
import com.alex.market.mvc.client.dto.TransactionStatus;
import com.alex.market.mvc.dto.output.PaymentApiStatus;
import com.alex.market.mvc.dto.output.PaymentResultDto;
import com.alex.market.mvc.mapper.PaymentApiMapper;
import com.alex.market.mvc.service.PaymentApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApiServiceImpl implements PaymentApiService {

    private final DefaultApi paymentApi;
    private final PaymentApiMapper paymentApiMapper;
    private static final Duration PAYMENT_TIMEOUT = Duration.ofSeconds(5);


    @Override
    public Mono<PaymentResultDto> processPaymentInTransaction(PaymentRequest paymentRequest) {

        return paymentApi.processPayment(paymentRequest)
                .timeout(PAYMENT_TIMEOUT)
                .map(response -> {
                    PaymentApiStatus paymentApiStatus =
                            response.getStatus() == TransactionStatus.SUCCESS
                                    ? PaymentApiStatus.SUCCESS
                                    : PaymentApiStatus.BUSINESS_FAIL;
                    log.info("Payment processing status: {}", paymentApiStatus);
                    return createPaymentResult(paymentApiStatus, response);
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Payment service error, HTTP-status: {}, body: {}",
                            e.getStatusCode().value(), e.getResponseBodyAsString());

                    return Mono.just(createErrorPaymentResult(PaymentApiStatus.SERVICE_ERROR));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Payment service is unavailable: {}", e.getMessage());

                    return Mono.just(createErrorPaymentResult(PaymentApiStatus.NETWORK_ERROR));
                });

    }

    private PaymentResultDto createPaymentResult(PaymentApiStatus status, PaymentResponse paymentResponse) {
        return new PaymentResultDto(status,
                paymentResponse.getOrderId(),
                paymentResponse.getTransactionId(),
                paymentResponse.getFailureReason());
    }

    private PaymentResultDto createErrorPaymentResult(PaymentApiStatus status) {
        return new PaymentResultDto(status,
                null,
                null,
                null
        );
    }

}


