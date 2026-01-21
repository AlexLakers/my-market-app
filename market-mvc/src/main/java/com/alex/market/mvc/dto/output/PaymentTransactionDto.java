package com.alex.market.mvc.dto.output;

import com.alex.market.mvc.client.dto.TransactionStatus;

public record PaymentTransactionDto(TransactionStatus transactionStatus, Long orderId, Long transactionId, String failureReason) {
}
