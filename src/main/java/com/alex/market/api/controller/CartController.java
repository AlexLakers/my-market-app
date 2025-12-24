package com.alex.market.api.controller;

import com.alex.market.api.dto.input.CartChangeDto;
import com.alex.market.api.dto.input.InputFormCart;
import com.alex.market.api.dto.output.CartDto;
import com.alex.market.api.dto.input.InputFormItem;
import com.alex.market.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;
    @GetMapping("/items")
    public String getItems(Model model,
                           @SessionAttribute Map<Long, Integer> cart){
        CartDto cartDto=cartService.getItems(cart);
        model.addAttribute("items", cartDto.items());
        model.addAttribute("total", cartDto.total());
        return "cart";
    }


    @PostMapping("/items")
    public String changeCartItemCountForCartPage(@Valid @ModelAttribute InputFormCart params,
                                                 @SessionAttribute @NotNull Map<Long, Integer> cart
    ){
        cartService.changeItemCount(new CartChangeDto(params.id(),params.action(),cart));
        return "redirect:/cart/items";
    }

}
