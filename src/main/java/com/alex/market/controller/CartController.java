package com.alex.market.controller;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.InputFormCart;
import com.alex.market.dto.output.CartDto;
import com.alex.market.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public Mono<Rendering> getItems(@SessionAttribute Map<Long, Integer> cart) {
        return cartService.getItemsCartWithTotal(cart)
                .map(cartDto -> Rendering.view("cart")
                        .modelAttribute("items", cartDto.items())
                        .modelAttribute("total", cartDto.total())
                        .build());
    }

    @PostMapping("/items")
    public Mono<String> changeCartItemCountForCartPage(@Valid @ModelAttribute InputFormCart params,
                                                       @SessionAttribute @NotNull Map<Long, Integer> cart
    ) {
        return cartService.changeItemCount(new CartChangeDto(params.id(), params.action(), cart))
                .thenReturn("redirect:/cart/items");

    }
}
