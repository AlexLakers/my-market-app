package com.alex.market.mvc.dto.output;

import java.math.BigDecimal;

public record AccountBalanceDto(Long accountId, Long balance, PaymentApiStatus status/*,boolean isEnough, boolean isPaymentAvailable*/) {
}
