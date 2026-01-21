package com.alex.market.mvc.mapper;

import com.alex.market.mvc.client.dto.PaymentResponse;
import com.alex.market.mvc.dto.output.PaymentTransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentApiMapper {

    @Mapping(source = "status", target = "transactionStatus")
    PaymentTransactionDto toPaymentTransactionDto(PaymentResponse paymentResponse);
}
