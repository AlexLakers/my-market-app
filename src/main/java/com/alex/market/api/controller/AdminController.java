package com.alex.market.api.controller;

import com.alex.market.api.dto.input.ItemCreateDto;
import com.alex.market.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
@Validated
public class AdminController {

    private final ItemService itemService;

    @GetMapping("/items")
    public String adminPage() {
        return "newItem";
    }

    @PostMapping(value = "/items/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createItem(@Valid @ModelAttribute ItemCreateDto item,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("title", item.title());
            redirectAttributes.addFlashAttribute("description", item.description());
            redirectAttributes.addFlashAttribute("price", item.price());
            return "redirect:/admin/items";
        }
        model.addAttribute("item", itemService.createItem(item));
        return "item";
    }
}
