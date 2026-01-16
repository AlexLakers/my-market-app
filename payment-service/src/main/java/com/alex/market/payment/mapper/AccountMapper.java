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
/*@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "price", source = "item.price")
    @Mapping(target = "count", source = "count")
    ItemDto toDto(Item item, Integer count);

    default ItemDto toDto(Item item, Map<Long, Integer> cart) {
        return toDto(item, cart.getOrDefault(item.getId(), 0));
    }

    ItemDto toDtoFromOrderItemDetails(OrderItemsDetails orderItemsDetails);
}*/
