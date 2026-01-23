package com.alex.market.payment.mapper;

import com.alex.market.payment.api.dto.PaymentResponse;
import com.alex.market.payment.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    @Mapping(source = "id", target = "transactionId")
    PaymentResponse toPaymentResponse(Transaction transaction);

}
