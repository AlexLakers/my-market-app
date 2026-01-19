package com.alex.market.mvc.dto.output;

import java.util.List;

public record CartDto(List<ItemDto> items, Long total, AccountBalanceDto accountBalance) {
}
