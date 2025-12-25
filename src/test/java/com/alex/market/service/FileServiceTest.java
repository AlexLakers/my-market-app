package com.alex.market.service;

import com.alex.market.exception.ImageStorageException;
import com.alex.market.service.impl.FileServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;


class FileServiceTest {

    @TempDir
    private Path baseDir;

    private FileService fileService;
    @BeforeEach
    void setUp() {
        this.fileService=new FileServiceImpl(baseDir);
    }

    @Test
    void saveFile_shouldReturnSavedPathFileSuccess(){
        byte[] content=new byte[]{0,1,2,3,4,5,6,7,8,9};

        MockMultipartFile givenFIle=new MockMultipartFile("image",content);
        fileService.saveFile(givenFIle,givenFIle.getName());

        Assertions.assertThat(Files.exists(Path.of(baseDir.toString(),"/images",givenFIle.getName()))).isTrue();
    }

    @Test
    void saveFile_shouldThrowImageStorageExceptionFail() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        Mockito.when(mockFile.getName()).thenReturn("test.jpg");
        Mockito.when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        Mockito.doThrow(new IOException("Simulated IO error")).when(mockFile).transferTo(Mockito.any(File.class));

        Assertions.assertThatThrownBy(() -> fileService.saveFile(mockFile, "test.jpg"))
                .isInstanceOf(ImageStorageException.class)
                .hasMessageContaining("images/test.jpg");
    }
    }