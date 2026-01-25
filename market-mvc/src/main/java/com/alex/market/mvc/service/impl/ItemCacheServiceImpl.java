package com.alex.market.mvc.service.impl;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.dto.output.ItemDto;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.service.ImageService;
import com.alex.market.mvc.service.ItemCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemCacheServiceImpl implements ItemCacheService {

    private final static String ITEM_DATA_PREFIX = "item:data:%d";
    private final static String ITEM_IMAGE_PREFIX = "item:image:%s";

    private final ReactiveRedisTemplate<String, ItemCache> reactiveRedisTemplate;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final ImageService imageService;
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    @Override
    public Mono<ItemDto> getItemById(Long id, Map<Long, Integer> cart) {

        String itemCacheKey = buildItemDataKey(id);
        return reactiveRedisTemplate.opsForValue().get(itemCacheKey)
                .switchIfEmpty(loadAndCacheItem(id))
                .flatMap(itemCache -> {
                            String imageCacheKey = buildItemImageKey(itemCache.imgPath());
                            return reactiveStringRedisTemplate.opsForValue().get(imageCacheKey)
                                    .switchIfEmpty(loadAndCacheImageForItem(itemCache))
                                    .map(imageUri ->
                                            itemMapper.toDtoFromItemCacheWithImage(itemCache, cart, imageUri)
                                    )
                                    .defaultIfEmpty(
                                            itemMapper.toDtoFromItemCacheWithoutImage(itemCache, cart)
                                    );
                        }
                );
    }




    private String handleImageBase64ToUri(String imageBase64, String imgPath) {
        return "data:image/" + imgPath.substring(imgPath.indexOf(".") + 1) + ";base64," + imageBase64;
    }

    private String buildItemDataKey(Long id) {
        return ITEM_DATA_PREFIX.formatted(id);
    }

    private String buildItemImageKey(String imgPath) {
        return ITEM_IMAGE_PREFIX.formatted(imgPath);
    }
}
