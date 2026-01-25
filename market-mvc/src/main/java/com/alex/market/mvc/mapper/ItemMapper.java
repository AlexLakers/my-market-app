package com.alex.market.mvc.mapper;


import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.projection.OrderItemsDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "title", source = "item.title")
    @Mapping(target = "description", source = "item.description")
    @Mapping(target = "imgPath", source = "item.imgPath")
    @Mapping(target = "price", source = "item.price")
    @Mapping(target = "count", source = "count")
    @Mapping(target = "imageAsBase64", source = "imageBase64")
    ItemDto toDto(Item item, Integer count, String imageBase64);


    default ItemDto toDto(Item item, Map<Long, Integer> cart, String imageBase64) {
        Integer count = cart.getOrDefault(item.getId(), 0);
        return toDto(item, count, imageBase64);
    }

    default ItemDto toDto(Item item, Map<Long, Integer> cart) {
        return toDto(item, cart, "");
    }

    ItemCache toCache(Item item);

    ItemDto toDtoFromOrderItemDetails(OrderItemsDetails orderItemsDetails);

    @Mapping(target = "imageAsBase64", defaultValue = "", source = "imageAsBase64")
    ItemDto toItemDtoFromCache(ItemCache itemCache, Integer count, String imageAsBase64);

    default ItemDto toDtoFromItemCacheWithImage(ItemCache itemCache, Map<Long, Integer> cart, String imageBase64) {
        Integer count = cart.getOrDefault(itemCache.id(), 0);
        return toItemDtoFromCache(itemCache, count, imageBase64);
    }
    default ItemDto toDtoFromItemCacheWithoutImage(ItemCache itemCache, Map<Long, Integer> cart) {
        Integer count = cart.getOrDefault(itemCache.id(), 0);
        return toItemDtoFromCache(itemCache, count, "");
    }

}

