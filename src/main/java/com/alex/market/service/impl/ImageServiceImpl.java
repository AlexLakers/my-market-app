package com.alex.market.service.impl;

import com.alex.market.config.ConfigProperties;
import com.alex.market.exception.ImageStorageException;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    private final ItemRepository itemRepository;
    private final ConfigProperties configProperties;

    @Override
    public Mono<Void> updateImageByItemId(FilePart file, Long id) {
        return validateFile(file)
                .then(itemRepository.existsById(id)
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .then(saveImage(file, id))
                .flatMap(imgPath -> itemRepository.updateImagePathById(id, imgPath)))
                .then();
    }

    private Mono<String> saveImage(FilePart file, Long id) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    try {
                        int readable = dataBuffer.readableByteCount();
                        if (readable <= 0) {
                            return Mono.error(new IllegalStateException("Пустой файл"));
                        }
                        if (readable > configProperties.getMaxSize()) {
                            return Mono.error(new IllegalArgumentException("Слишком большой файл"));
                        }

                        byte[] bytes = new byte[readable];
                        dataBuffer.read(bytes);
                        return null;
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                });
    }

    private Mono<Void> validateFile(FilePart file) {
        return Mono.fromCallable(() -> {
            MediaType contentType = file.headers().getContentType();
            if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {
                throw new IllegalArgumentException("Нужен файл изображения");
            }
            return null;
        });
    }


}

