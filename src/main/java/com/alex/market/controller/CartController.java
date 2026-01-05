package com.alex.market.controller;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.InputFormCart;
import com.alex.market.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public Mono<String> changeCartItemCountForCartPage(@Valid @ModelAttribute InputFormCart params,
                                                       @SessionAttribute @NotNull Map<Long, Integer> cart
    ) {
        return cartService.changeItemCount(new CartChangeDto(params.id(), params.action(), cart))
                .thenReturn("redirect:/cart/items");

    }
}
