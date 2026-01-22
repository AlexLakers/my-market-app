package com.alex.market.mvc.dto.output;

public record PaymentDto(PaymentApiStatus status, Long orderId, Long transactionId, String failureReason) {
}
