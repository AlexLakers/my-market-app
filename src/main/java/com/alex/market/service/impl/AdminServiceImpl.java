package com.alex.market.service.impl;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.exception.TitleAlreadyExistsException;
import com.alex.market.mapper.ItemMapper;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.service.AdminService;
import com.alex.market.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final FileService fileService;
    @Override
    public ItemDto createItem(ItemCreateDto itemCreateDto) {

        if(itemRepository.existsByTitle(itemCreateDto.title())) {
            throw new TitleAlreadyExistsException(itemCreateDto.title());
        }

        Item savedItem=itemRepository.save(toItem(itemCreateDto/*, imagePath*/));
        return itemMapper.toDto(savedItem,new HashMap<>());
    }

    @Override
    @Transactional
    public void uploadImage(MultipartFile image, Long id) {
        String imageName=generateNewImagePath(id,image.getOriginalFilename());
        System.out.println(image.getOriginalFilename());
        System.out.println(imageName);
        String imagePath=fileService.saveFile(image, imageName);

        itemRepository.updateImagePathById(id,imagePath);
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

    private Item toItem(ItemCreateDto dto/*String imgPath*/){
        return Item.builder().title(dto.title()).description(dto.description()).price(dto.price())./*imgPath(imgPath).*/build();
    }
}
