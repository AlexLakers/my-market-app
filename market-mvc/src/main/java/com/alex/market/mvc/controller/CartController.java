package com.alex.market.mvc.controller;

import com.alex.market.mvc.dto.input.CartChangeDto;
import com.alex.market.mvc.dto.input.InputFormCart;
import com.alex.market.mvc.dto.output.CartDto;
import com.alex.market.mvc.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public Mono<Rendering> getItemsCartWithTotal(@SessionAttribute Map<Long, Integer> cart) {
        log.info("---endpoint 'getItemsCartWithTotal' with cart:{} from session was started---", cart);

        return cartService.getItemsCartWithTotal(cart)
                .map(cartDto -> Rendering.view("cart")
                        .modelAttribute("items", cartDto.items())
                        .modelAttribute("total", cartDto.total())
                        .status(HttpStatus.OK)
                        .build());
    }

    @PostMapping("/items")
    public Mono<String> changeCartItemCountForCartPage(@Valid @ModelAttribute InputFormCart params,
                                                       @SessionAttribute @NotNull Map<Long, Integer> cart
    ) {
        log.info("---endpoint 'changeCartItemCountForCartPage' with input form params:{} and cart from session:{} was started---",
                params, cart);

        return cartService.changeItemCount(new CartChangeDto(params.id(), params.action(), cart))
                .thenReturn("redirect:/cart/items");
    }
}
