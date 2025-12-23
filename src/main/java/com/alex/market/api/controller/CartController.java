package com.alex.market.api.controller;

import com.alex.market.api.dto.CartChangeDto;
import com.alex.market.api.dto.CartDto;
import com.alex.market.model.CartAction;
import com.alex.market.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    @GetMapping("/items")
    public String getItems(Model model,  @SessionAttribute Map<Long, Integer> cart){
        CartDto cartDto=cartService.getItems(cart);
        model.addAttribute("items", cartDto.items());
        model.addAttribute("total", cartDto.total());
        return "cart";
    }


    @PostMapping("/items")
    public String changeCartItemCountForCartPage(@RequestParam Long id,
                                                 @RequestParam CartAction action,
                                                 @SessionAttribute Map<Long, Integer> cart,
                                                 Model model
    ){
        cartService.changeItemCount(new CartChangeDto(id,action,cart));
        return "redirect:/cart/items";
    }

}
