package com.alex.market.mvc.service.impl;

import com.alex.market.mvc.config.ConfigProperties;
import com.alex.market.mvc.exception.ImageGettingException;
import com.alex.market.mvc.exception.ImageStorageException;
import com.alex.market.mvc.exception.ItemNotFoundException;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.service.ImageService;
import com.alex.market.mvc.validation.ValidMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {
    private final ItemRepository itemRepository;
    private final ConfigProperties configProperties;


    @Override
    public Mono<Void> updateImageByItemId(FilePart file, Long id) {
        log.info("Update image for item with id: {}", id);

        return itemRepository.existsById(id)
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .then(saveImage(file, id))
                .flatMap(imgPath -> itemRepository.updateImagePathById(id, imgPath))
                .then()
                .doOnSuccess(unused -> log.info("Image updated for item with id: {}", id))
                .doOnError(error -> {
                    if (error instanceof ItemNotFoundException) {
                        log.warn("Item with id: {} not found for image update", id);
                    } else {
                        log.error("Failed to update image for item with id: {}: {}", id, error.getMessage());
                    }
                });
    }

    @Override
    public Mono<byte[]> getImageByImgPath(String imgPath) {
        if (imgPath == null || imgPath.isEmpty()) {
            return Mono.empty();
        }

        return Mono.defer(() -> {
            try {
                Path baseDir = configProperties.getDir();
                Path fullPath = baseDir.resolve(imgPath);

                if (!Files.exists(fullPath)) {
                    log.warn("Image file does not exist: {}", fullPath);
                    return Mono.empty();
                }

                try (InputStream inputStream = Files.newInputStream(fullPath)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(data)) != -1) {
                        buffer.write(data, 0, bytesRead);
                    }

                    byte[] result = buffer.toByteArray();

                    if (result.length == 0) {
                        log.warn("Image file is empty: {}", fullPath);
                        return Mono.empty();
                    }

                    return Mono.just(result);
                }
            } catch (IOException e) {
                log.error("Failed to read image file: {}", imgPath, e);
                return Mono.empty();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> saveImage(FilePart file, Long id) {
        log.debug("Save image for item {}: {}", id, file.filename());

        String fileName = generateNewImagePath(id, file.filename());
        Path baseDir = configProperties.getDir();
        Path imagesDir = baseDir.resolve("images");
        Path fullPath = imagesDir.resolve(fileName);

        try {
            Files.createDirectories(imagesDir);
        } catch (IOException e) {
            return Mono.error(new ImageStorageException("Failed to create directory: " + e.getMessage()));
        }

        return file.transferTo(fullPath)
                .then(Mono.fromCallable(() -> {

                    long fileSize = Files.size(fullPath);
                    log.debug("File saved, size: {} bytes", fileSize);

                    if (fileSize <= 0) {
                        Files.deleteIfExists(fullPath);
                        throw new ImageStorageException(ValidMessages.FILE_EMPTY);
                    }

                    if (fileSize > configProperties.getMaxSize()) {
                        Files.deleteIfExists(fullPath);
                        throw new ImageStorageException(ValidMessages.FILE_TOO_BIG);
                    }

                    return baseDir.relativize(fullPath).toString();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(path ->
                        log.info("Image saved for item with id: {} at {}", id, path)
                )
                .doOnError(error ->
                        log.error("Failed to save image for item with id: {}: {}", id, error.getMessage())
                );
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
}

