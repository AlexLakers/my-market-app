package com.alex.market.controller;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.service.ImageService;
import com.alex.market.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
@Validated
public class AdminController {

    private final ItemService itemService;
    private final ImageService imageService;

    @GetMapping("/items")
    public String adminPage() {
        return "newItem";
    }

    @PostMapping(value = "/items/add")
    public String createItem(@Valid @ModelAttribute ItemCreateDto item,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("title", item.title());
            redirectAttributes.addFlashAttribute("description", item.description());
            redirectAttributes.addFlashAttribute("price", item.price());
            return "redirect:/admin/items";
        }
        ItemDto itemDto = itemService.createItem(item);
        redirectAttributes.addFlashAttribute("item", itemDto);
        return "redirect:/admin/images";
    }

    @GetMapping("/images")
    public String imagePage() {
        return "newImage";
    }

    @PostMapping(value = "/images/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) {
        imageService.uploadImage(image, id);
        return "redirect:/items/{id}";
    }

}
