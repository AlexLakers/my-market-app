package com.alex.market.service;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import org.springframework.web.multipart.MultipartFile;

public interface AdminService {
    ItemDto createItem(ItemCreateDto itemCreateDto);

    void uploadImage (MultipartFile file, Long id);
}
