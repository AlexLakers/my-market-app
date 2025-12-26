package com.alex.market.service;

import com.alex.market.exception.ImageStorageException;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.impl.ImageServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {
    private final static Long VALID_ID=1L;
    private final static Long INVALID_ID=10000L;
    @TempDir
    private Path baseDir;

    @Mock
    private ItemRepository itemRepository;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageServiceImpl(baseDir, itemRepository);
    }

    @Test
    void uploadImage_shouldReturnSavedPathFileSuccess(){
        byte[] content = new byte[]{0,1,2,3,4,5,6,7,8,9};
        MockMultipartFile givenFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", content);
        when(itemRepository.existsById(VALID_ID)).thenReturn(true);
        doNothing().when(itemRepository).updateImagePathById(anyLong(), anyString());

        imageService.uploadImage(givenFile, VALID_ID);

        Path imagesDir = baseDir.resolve("images");
        assertThat(Files.exists(imagesDir)).isTrue();
    }

    @Test
    void uploadImage_shouldThrowImageStorageExceptionFail() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("image.jpeg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(itemRepository.existsById(INVALID_ID)).thenReturn(true);
        doThrow(new IOException("Simulated IO error"))
                .when(mockFile).transferTo(any(Path.class));

        assertThatThrownBy(() -> imageService.uploadImage(mockFile, INVALID_ID))
                .isInstanceOf(ImageStorageException.class);
    }
    @Test
    void uploadImage_shouldThrowItemNotFoundExceptionFail() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(itemRepository.existsById(INVALID_ID)).thenReturn(false);

        assertThatThrownBy(() -> imageService.uploadImage(mockFile, INVALID_ID))
                .isInstanceOf(ItemNotFoundException.class);
    }

    }