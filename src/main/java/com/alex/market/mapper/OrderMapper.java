package com.alex.market.mapper;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.model.Item;
import com.alex.market.model.Order;
import com.alex.market.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface OrderMapper {

    default OrderDto toDto(Order order, List<OrderItem> orderItems, List<Item> items) {

        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        List<ItemDto> itemDtos = orderItems.stream()
                .map(orderItem -> {
                    Item item = itemMap.get(orderItem.getItemId());
                    return createItemDto(orderItem, item);
                })
                .collect(Collectors.toList());

        return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
    }


    private ItemDto createItemDto(OrderItem orderItem, Item item) {
        if (item == null) {
            return new ItemDto(orderItem.getItemId(), null, null, null, orderItem.getHistoryPrice(), orderItem.getCount());
        }

        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(), item.getImgPath(), orderItem.getHistoryPrice(), orderItem.getCount());
    }
}