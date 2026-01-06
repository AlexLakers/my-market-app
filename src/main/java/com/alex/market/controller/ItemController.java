package com.alex.market.controller;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.InputFormItem;
import com.alex.market.dto.input.InputFormItems;
import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ImageService;
import com.alex.market.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.codec.multipart.FilePart;
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
@Validated
public class ItemController {
    private final ItemService itemService;
    private final ImageService imageService;

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
                        .status(HttpStatus.OK)
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
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        }
        return itemService.createItem(item)
                .map(savedItemDto -> Rendering.view("newImage")
                        .modelAttribute("item", savedItemDto)
                        .status(HttpStatus.CREATED)
                        .build());

    }

    @GetMapping("/items/images/new")
    public Mono<String> showNewImagePage() {
        return Mono.just("newImage");
    }

    @PostMapping("/items/{id}/images/new")
    public Mono<String> updateImageById(@PathVariable Long id, @RequestPart FilePart image) {
        return imageService.updateImageByItemId(image, id).thenReturn("redirect:/items/" + id);
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> getItem(@PathVariable Long id,
                                   @SessionAttribute("cart") Map<Long, Integer> cart) {
        return itemService.getItemByIdWithCartCount(id, cart)
                .map(dto -> Rendering.view("item")
                        .modelAttribute("item", dto)
                        .status(HttpStatus.OK)
                        .build());
    }

    @PostMapping("/items")
    public Mono<String> changeCartItemCountForItemsPage(@Valid @ModelAttribute InputFormItems params,
                                                        @SessionAttribute Map<Long, Integer> cart
    ) {

        return itemService.changeCartItemCount(new CartChangeDto(params.id(), params.action(), cart))
                .thenReturn("redirect:/items?search=" + params.search()
                            + "&sort=" + params.sort()
                            + "&pageSize=" + params.pageSize()
                            + "&pageNumber=" + params.pageNumber());

    }


    @PostMapping("/items/{id}")
    public Mono<Rendering> changeCartItemCountForItemPage(@ModelAttribute InputFormItem params,
                                                          @SessionAttribute Map<Long, Integer> cart
    ) {
        return itemService.changeCartItemCount(new CartChangeDto(params.id(), params.action(), cart))
                .map(itemDto -> Rendering
                        .view("item")
                        .modelAttribute("item", itemDto)
                        .status(HttpStatus.OK)
                        .build());

    }

}