package com.alex.market.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    void updateImageByItemId(MultipartFile file, Long id);
}
