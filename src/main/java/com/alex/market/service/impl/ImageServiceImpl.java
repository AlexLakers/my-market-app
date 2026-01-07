package com.alex.market.service.impl;

import com.alex.market.config.ConfigProperties;
import com.alex.market.exception.ImageStorageException;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.ImageService;
import com.alex.market.validation.ValidMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private Mono<String> saveImage(FilePart file, Long id) {
        log.debug("Save image for item {}: {}", id, file.filename());

        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    try {

                        MediaType contentType = file.headers().getContentType();
                        if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {

                            log.warn("Non-image file type for item with id: {}: {}", id, contentType);
                            return Mono.error(new ImageStorageException(ValidMessages.FILE_NOT_IMAGE));
                        }

                        int readable = dataBuffer.readableByteCount();
                        log.debug("File size: {} bytes", readable);

                        if (readable <= 0) {
                            log.warn("Empty file for item with id: {}", id);

                            return Mono.error(new ImageStorageException(ValidMessages.FILE_EMPTY));
                        }
                        if (readable > configProperties.getMaxSize()) {

                            log.warn("File too large for item with id:{}: {} bytes", id, readable);
                            return Mono.error(new ImageStorageException(ValidMessages.FILE_TOO_BIG));
                        }

                        byte[] bytes = new byte[readable];
                        dataBuffer.read(bytes);

                        String fileName = generateNewImagePath(id, file.filename());

                        log.debug("Generated safe filename: {}", fileName);
                        return saveToFileSystem(bytes, fileName)
                                .doOnSuccess(path ->
                                        log.info("Image saved for item with id: {} {}", id, path)
                                )
                                .doOnError(error ->
                                        log.error("Failed to save image for item with id:{} {}", id, error.getMessage())
                                );
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                })
                .doOnError(ImageStorageException.class, error ->
                        log.warn("Image storage exception for item with id: {}: {}", id, error.getMessage())
                )
                .doOnError(error ->
                        log.error("Unexpected error saving image for item with id: {}: {}", id, error.getMessage())
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

private Mono<String> saveToFileSystem(byte[] content, String fileName) {
    Path baseDir = configProperties.getDir();
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

