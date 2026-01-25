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


    private Mono<String> loadAndCacheImageForItem(ItemCache itemCache) {
            if (itemCache.imgPath() == null || itemCache.imgPath().isEmpty()) {
                return Mono.empty();
            }

            String imageCacheKey = buildItemImageKey(itemCache.imgPath());

            return imageService.getImageByImgPath(itemCache.imgPath())
                    .filter(imageBytes -> imageBytes != null && imageBytes.length > 0)
                    .flatMap(imageBytes -> {
                        String imageAsBase64 = Base64.getEncoder().encodeToString(imageBytes);
                        String imageUri = handleImageBase64ToUri(imageAsBase64, itemCache.imgPath());

                        return reactiveStringRedisTemplate.opsForValue()
                                .set(imageCacheKey, imageUri, CACHE_TTL)
                                .doOnNext(isSet -> {
                                    if (Boolean.TRUE.equals(isSet)) {
                                        log.debug("Cached image for item: {} with key: {}",
                                                itemCache.id(), imageCacheKey);
                                    } else {
                                        log.warn("Failed to cache image for item: {}", itemCache.id());
                                    }
                                })
                                .thenReturn(imageUri);
                    })
                    .onErrorResume(e -> {
                        log.warn("Failed to load image for item {}: {}",
                                itemCache.id(), e.getMessage());
                        return Mono.empty();
                    });
        }



    private Mono<ItemCache> loadAndCacheItem(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .map(item -> {
                    log.debug("Loading item with id: {} from database:", id);
                    return itemMapper.toCache(item);
                })
                .flatMap(itemCache -> {
                    String cacheKey = buildItemDataKey(id);

                    return reactiveRedisTemplate.opsForValue().set(cacheKey,itemCache, CACHE_TTL)
                                    .doOnNext(isSet -> {
                                        if (Boolean.TRUE.equals(isSet)) {
                                            log.debug("Cached item with id: {}", id);
                                        } else {
                                            log.warn("Failed to cache item with id: {}", id);
                                        }
                                    })
                                    .onErrorResume(e -> {
                                        log.error("Cache failed for item with id: {}: {}", id, e.getMessage());

                                        return Mono.empty();
                                    })
                                    .thenReturn(itemCache);
                });
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
