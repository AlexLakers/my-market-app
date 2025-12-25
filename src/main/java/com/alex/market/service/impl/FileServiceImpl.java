package com.alex.market.service.impl;

import com.alex.market.exception.ImageStorageException;
import com.alex.market.service.FileService;
import lombok.RequiredArgsConstructor;
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
    private Path baseDir;

    public FileServiceImpl(@Value("${market.upload.dir:/home/alexlakers/my-market}") Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String saveFile(MultipartFile file, String fileName) {
        Path fullPath = Path.of(baseDir.toString(), "images", fileName);
        Path relativePath = baseDir.relativize(fullPath);
        try {
            Files.createDirectories(fullPath.getParent());

            file.transferTo(fullPath.toFile());
        } catch (Exception e) {
            throw new ImageStorageException(fullPath.toString());
        }
        return relativePath.toString();
    }
}