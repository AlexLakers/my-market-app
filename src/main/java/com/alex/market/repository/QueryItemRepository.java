package com.alex.market.repository;

import com.alex.market.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface QueryItemRepository {
    Mono<Page<Item>> findAll(String search, Pageable pageable);
    Mono<Void> updateImagePathById(Long id, String imagePath);
}
