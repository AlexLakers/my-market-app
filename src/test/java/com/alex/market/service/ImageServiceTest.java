package com.alex.market.service;

import com.alex.market.config.ConfigProperties;
import com.alex.market.exception.ImageStorageException;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.impl.ImageServiceImpl;
import com.alex.market.validation.ValidMessages;
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
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private final static Long VALID_ID = 1L;
    private final static Long INVALID_ID = 10000L;
    @TempDir
    private Path baseDir;
    private static final int MAX_BYTES = 5242880;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private FilePart filePart;
    private ImageService imageService;


    @BeforeEach
    void setUp() {
        ConfigProperties configProperties = new ConfigProperties();
        configProperties.setDir(baseDir);
        configProperties.setMaxSize(MAX_BYTES);
        imageService = new ImageServiceImpl(itemRepository, configProperties);
    }


    @Test
    void updateImageByItemId_shouldSaveImageInTempDirSuccess() throws IOException {
        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
        DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(imageBytes);

        when(filePart.filename()).thenReturn("image.jpeg");
        when(filePart.content()).thenReturn(Flux.just(dataBuffer));
        when(filePart.headers()).thenReturn(new HttpHeaders() {{
            setContentType(MediaType.IMAGE_JPEG);
            setContentLength(imageBytes.length);
        }});

        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));
        when(itemRepository.updateImagePathById(eq(VALID_ID), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(imageService.updateImageByItemId(filePart, VALID_ID))
                .verifyComplete();


        verify(itemRepository).existsById(VALID_ID);
        verify(itemRepository).updateImagePathById(eq(VALID_ID), anyString());


        Path expectedFile = baseDir.resolve("images").resolve("1.jpeg");
        assertThat(Files.exists(expectedFile)).isTrue();
    }


    @Test
    void updateImageByItemId_whenDirectoryDoesNotExist_shouldCreateIt() {
        // Arrange
        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
        DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(imageBytes);

        when(filePart.filename()).thenReturn("image.jpeg");
        when(filePart.content()).thenReturn(Flux.just(dataBuffer));
        when(filePart.headers()).thenReturn(new HttpHeaders() {{
            setContentType(MediaType.IMAGE_JPEG);
            setContentLength(imageBytes.length);
        }});

        when(itemRepository.existsById(VALID_ID)).thenReturn(Mono.just(true));
        when(itemRepository.updateImagePathById( eq(VALID_ID),anyString()))
                .thenReturn(Mono.empty());

        Path imagesDir = baseDir.resolve("images");
        try {
            Files.deleteIfExists(imagesDir);
        } catch (Exception e) {

        }

        // Act & Assert
        StepVerifier.create(imageService.updateImageByItemId(filePart, VALID_ID))
                .verifyComplete();

        // Verify directory was created
        assertThat(Files.exists(imagesDir)).isTrue();
        assertThat(Files.isDirectory(imagesDir)).isTrue();
    }

}