package com.alex.market.mvc.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface ImageService {
    Mono<Void> updateImageByItemId(FilePart file, Long id);
}
