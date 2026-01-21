package com.alex.market.mvc.dto.output;

public record PaymentResultDto(PaymentApiStatus status, Long orderId, Long transactionId,String failureReason) {
}
