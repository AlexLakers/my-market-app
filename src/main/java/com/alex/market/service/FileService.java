package com.alex.market.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

public interface FileService {
    String saveFile(MultipartFile file, String fileName);

}
