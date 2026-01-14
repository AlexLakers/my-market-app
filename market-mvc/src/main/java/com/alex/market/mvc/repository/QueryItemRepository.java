package com.alex.market.mvc.repository;

import com.alex.market.mvc.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface QueryItemRepository {
    Mono<Page<Item>> findAll(String search, Pageable pageable);
    Mono<Void> updateImagePathById(Long id, String imagePath);
}
