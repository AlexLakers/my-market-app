package com.alex.market.api.controller;

import com.alex.market.api.dto.CartDto;
import com.alex.market.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

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
}
