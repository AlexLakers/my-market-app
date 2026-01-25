package com.alex.market.mvc.service;

import com.alex.market.mvc.dto.output.ItemDto;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ItemCacheService {
    Mono<ItemDto> getItemById(Long id, Map<Long, Integer> map);
}
