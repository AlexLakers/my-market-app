package com.alex.market.payment.mapper;

import com.alex.market.payment.api.dto.AccountResponse;
import com.alex.market.payment.model.Account;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Map;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {
   @Mapping(source = "id", target = "accountId")
   AccountResponse toAccountResponse (Account account);
}
