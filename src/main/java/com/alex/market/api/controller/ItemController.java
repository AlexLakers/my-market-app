package com.alex.market.api.controller;

import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = {"/", "/items"})
public class ItemController {
    private final ItemService itemService;

    @GetMapping
    public String getItems(@RequestParam(required = false) String search,
                           @RequestParam(required = false, defaultValue = "NO") SortColumn sort,
                           @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                           @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                           @SessionAttribute Map<Long, Integer> cart,
                           Model model) {


        PageItemsDto pageItemsDto = itemService.getItemsPage(new SearchDto(search, sort, pageNumber, pageSize, cart));

        model.addAttribute("items", pageItemsDto.items());
        model.addAttribute("search", pageItemsDto.search());
        model.addAttribute("sort", pageItemsDto.sort());
        model.addAttribute("paging", pageItemsDto.pageDto());

        return "items";
    }

}
