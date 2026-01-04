package com.alex.market.controller;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
//@Validated
public class ItemController {
    private final ItemService itemService;

    @GetMapping(value = {"/", "/items"})
    public Mono<Rendering> getItems(@RequestParam(required = false) String search,
                                    @RequestParam(required = false, defaultValue = "NO") SortColumn sort,
                                    @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                    @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                    @SessionAttribute("cart") Map<Long, Integer> cart) {

        return itemService.getItemsPage(new SearchDto(search, sort, pageNumber, pageSize, cart))
                .map(pageItemsDto -> Rendering.view("items")
                        .modelAttribute("items", pageItemsDto.items())
                        .modelAttribute("search", pageItemsDto.search())
                        .modelAttribute("sort", pageItemsDto.sort())
                        .modelAttribute("paging", pageItemsDto.pageDto())
                        .build());
    }

    @GetMapping("/items/new")
    public Mono<String> showNewItemPage() {
        return Mono.just("newItem");
    }


    @PostMapping(value = "/items/new")
    public Mono<Rendering> createItem(@Validated @ModelAttribute ItemCreateDto item,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {

            return Mono.just(Rendering.view("newItem")
                    .modelAttribute("errors", bindingResult.getAllErrors())
                    .modelAttribute("title", item.title())
                    .modelAttribute("description", item.description())
                    .modelAttribute("price", item.price())
                    .build());
        }
        return itemService.createItem(item)
                .map(savedItemDto -> Rendering.view("newImage")
                        .modelAttribute("item", savedItemDto).build());

    }
}