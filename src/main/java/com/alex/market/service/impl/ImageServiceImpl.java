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

                        String fileName = generateNewImagePath(id, file.filename());
                        return saveToFileSystem(bytes, fileName);
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

    private String generateNewImagePath(Long id, String origName) {
        String type = getTypeFromFileName(origName);
        return id + type;
    }

    private String getTypeFromFileName(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".")))
                .orElse("");
    }

    private Mono<String> saveToFileSystem(byte[] content, String fileName) {
        Path baseDir=configProperties.getDir();
        return Mono.fromCallable(() -> {
            Path imagesDir = baseDir.resolve("images");
            Path fullPath = imagesDir.resolve(fileName);
            try {
                Files.createDirectories(fullPath.getParent());

                Files.write(fullPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return baseDir.relativize(fullPath).toString();
            } catch (Exception e) {
                throw new ImageStorageException(fullPath.toString());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}

