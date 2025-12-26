package com.alex.market.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    void uploadImage (MultipartFile file, Long id);
}
