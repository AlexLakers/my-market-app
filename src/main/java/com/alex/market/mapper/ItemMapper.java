package com.alex.market.mapper;


import com.alex.market.dto.output.ItemDto;
import com.alex.market.model.Item;
import com.alex.market.model.OrderItem;
import com.alex.market.repository.projection.OrderItemsDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "price", source = "item.price")
    @Mapping(target = "count", source = "count")
    ItemDto toDto(Item item, Integer count);

    default ItemDto toDto(Item item, Map<Long, Integer> cart) {
        return toDto(item, cart.getOrDefault(item.getId(), 0));
    }

    ItemDto toDtoFromOrderItemDetails(OrderItemsDetails orderItemsDetails);
}

