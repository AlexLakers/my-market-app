package com.alex.market.service.impl;

import com.alex.market.exception.ImageStorageException;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class ImageServiceImpl implements ImageService {

    private ItemRepository itemRepository;
    private Path baseDir;

    public ImageServiceImpl(@Value("${market.upload.dir:/home/alexlakers/my-market}") Path baseDir,
                            ItemRepository itemRepository) {
        this.baseDir = baseDir;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public void uploadImage(MultipartFile image, Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }

        String imageName = generateNewImagePath(id, image.getOriginalFilename());
        String imagePath = saveFile(image, imageName);

        itemRepository.updateImagePathById(id, imagePath);
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

    private String saveFile(MultipartFile file, String fileName) {
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

