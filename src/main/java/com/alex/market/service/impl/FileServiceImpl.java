package com.alex.market.service.impl;

import com.alex.market.exception.ImageStorageException;
import com.alex.market.service.FileService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileServiceImpl implements FileService {
    @Value("${market.upload.dir:/home/alexlakers/my-marke}")
    private String baseDir;

    @Override
    @SneakyThrows
    public String saveFile(MultipartFile file, String fileName) {
        Path fullPath = Path.of(baseDir,"images", fileName);
        Path relativePath=Path.of(baseDir).relativize(fullPath);
        Files.createDirectories(fullPath.getParent());
        try {
            file.transferTo(fullPath.toFile());
        }
        catch (Exception e) {
            throw new ImageStorageException(fullPath.toString());
        }
        return relativePath.toString();
    }
}