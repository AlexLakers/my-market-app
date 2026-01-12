package com.alex.market.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface ImageService {
    Mono<Void> updateImageByItemId(FilePart file, Long id);
}
