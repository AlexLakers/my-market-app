package com.alex.market.api.controller;

import com.alex.market.api.dto.CartChangeDto;
import com.alex.market.model.CartAction;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
//@RequestMapping(value = {"/", "/items"})
public class ItemController {
    private final ItemService itemService;

    @GetMapping(value = {"/", "/items"})
    public String getItems( @RequestParam(required = false) String search,
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
    public String getItemById( @PathVariable Long id,
                              @SessionAttribute Map<Long, Integer> cart,
                              Model model) {
        model.addAttribute("item",itemService.findByIdWithCartCount(id,cart));
        return "item";
    }

    @PostMapping("/items")
    public String changeCartItemCountForItemsPage(@RequestParam(required = true) Long id,
                                                  @RequestParam(required = false) String search,
                                                  @RequestParam(required = false, defaultValue = "NO") SortColumn sort,
                                                  @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                  @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = true) CartAction action,
                                                  @SessionAttribute Map<Long, Integer> cart,
                                                  RedirectAttributes redirectAttributes

    ){
        itemService.changeCartItemCount(new CartChangeDto(id,action,cart));
        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("pageSize", pageSize);
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        return "redirect:/items";
    }

    @PostMapping("/items/{id}")
    public String changeCartItemCountForItemPage(@PathVariable Long id,
                                                 @RequestParam CartAction action,
                                                 @SessionAttribute Map<Long, Integer> cart,
                                                 Model model
                                                 ){
        model.addAttribute("item",itemService.changeCartItemCount(new CartChangeDto(id,action,cart)));

        return "item";
    }

}
