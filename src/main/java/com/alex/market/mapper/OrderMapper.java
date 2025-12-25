package com.alex.market.mapper;

import com.alex.market.api.dto.output.OrderDto;
import com.alex.market.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "items", source = "orderItems")
    OrderDto toDto(Order order);
}
