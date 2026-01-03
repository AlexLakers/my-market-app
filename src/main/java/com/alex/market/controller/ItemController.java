package com.alex.market.controller;

import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
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
                        .modelAttribute( "sort", pageItemsDto.sort())
                        .modelAttribute( "paging", pageItemsDto.pageDto())
                        .build());
    }
}