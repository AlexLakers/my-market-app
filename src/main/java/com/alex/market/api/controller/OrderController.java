package com.alex.market.api.controller;

import com.alex.market.api.dto.output.OrderDto;
import com.alex.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public String createOrder(@SessionAttribute Map<Long, Integer> cart) {
        Long savedId = orderService.createOrder(cart).id();
        cart.clear();
        return "redirect:/orders/" + savedId + "?newOrder=true";
    }


}
