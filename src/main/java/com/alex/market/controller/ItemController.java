package com.alex.market.controller;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.InputFormItem;
import com.alex.market.dto.input.InputFormItems;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Validated
//@RequestMapping(value = {"/", "/items"})
public class ItemController {
    private final ItemService itemService;

    @GetMapping(value = {"/", "/items"})
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

    @GetMapping(value = "/items/{id}")
    public String getItemById(@PathVariable Long id,
                              @SessionAttribute Map<Long, Integer> cart,
                              Model model) {
        model.addAttribute("item", itemService.findByIdWithCartCount(id, cart));
        return "item";
    }

    @PostMapping("/items")
    public String changeCartItemCountForItemsPage(@Valid @ModelAttribute InputFormItems params,
                                                  @SessionAttribute Map<Long, Integer> cart,
                                                  RedirectAttributes redirectAttributes

    ) {
        itemService.changeCartItemCount(new CartChangeDto(params.id(), params.action(), cart));
        redirectAttributes.addAttribute("search", params.search());
        redirectAttributes.addAttribute("sort", params.sort());
        redirectAttributes.addAttribute("pageSize", params.pageSize());
        redirectAttributes.addAttribute("pageNumber", params.pageNumber());
        return "redirect:/items";
    }

    @PostMapping("/items/{id}")
    public String changeCartItemCountForItemPage(@ModelAttribute InputFormItem params,
                                                 @SessionAttribute Map<Long, Integer> cart,
                                                 Model model
    ) {
        model.addAttribute("item", itemService.changeCartItemCount(new CartChangeDto(params.id(), params.action(), cart)));

        return "item";
    }

}
