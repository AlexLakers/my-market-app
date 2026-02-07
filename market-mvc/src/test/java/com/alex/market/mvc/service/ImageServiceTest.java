package com.alex.market.mvc.service;

import com.alex.market.mvc.config.ConfigProperties;
import com.alex.market.mvc.exception.ImageStorageException;
import com.alex.market.mvc.repository.ItemRepository;
import com.alex.market.mvc.service.impl.ImageServiceImpl;
import com.alex.market.mvc.validation.ValidMessages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ConfigProperties configProperties;

    @InjectMocks
    private ImageServiceImpl imageService;

    @TempDir
    Path tempDir;

    private static final Long VALID_ID = 1L;

    @BeforeEach
    void setUp() {
        when(configProperties.getDir()).thenReturn(tempDir);
        when(configProperties.getMaxSize()).thenReturn(1024);
    }

    @Test
    void updateImageByItemId_shouldSaveFileWithCorrectExtension() throws IOException {
        byte[] imageBytes = createTestJpeg();
        FilePart filePart = createFilePart("test.jpg", imageBytes, MediaType.IMAGE_JPEG);

        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));
        when(itemRepository.updateImagePathById(eq(VALID_ID), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(imageService.updateImageByItemId(filePart, VALID_ID))
                .verifyComplete();

        Path savedFile = tempDir.resolve("images").resolve("1.jpg");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    void updateImageByItemId_shouldSavePngFile() throws IOException {
        byte[] imageBytes = createTestPng();
        FilePart filePart = createFilePart("test.png", imageBytes, MediaType.IMAGE_PNG);

        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));
        when(itemRepository.updateImagePathById(eq(VALID_ID), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(imageService.updateImageByItemId(filePart, VALID_ID))
                .verifyComplete();

        Path savedFile = tempDir.resolve("images").resolve("1.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    private byte[] createTestJpeg() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] createTestPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private FilePart createFilePart(String filename, byte[] content, MediaType mediaType) {
        return new FilePart() {
            @Override
            public String filename() { return filename; }

            @Override
            public Mono<Void> transferTo(Path dest) {
                return Mono.fromCallable(() -> {
                    Files.createDirectories(dest.getParent());
                    Files.write(dest, content);
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()).then();
            }

            @Override
            public String name() { return "file"; }

            @Override
            public HttpHeaders headers() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(mediaType);
                return headers;
            }

            @Override
            public Flux<DataBuffer> content() {
                DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(content);
                return Flux.just(buffer);
            }

            @Override
            public Mono<Void> delete() { return Mono.empty(); }
        };
    }
}