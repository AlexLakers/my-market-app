package com.alex.market.api.controller;

import com.alex.market.api.dto.CartDto;
import com.alex.market.api.dto.ItemDto;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebMvcTest(CartController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class CartControllerTest {

    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1L;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
    }

    @MockitoBean(reset = MockReset.BEFORE)
    private CartService cartService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getItems_shouldReturnViewAndModelWithDataFromCart() throws Exception {
        ItemDto itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID));
        CartDto expectedDto=new CartDto(List.of(itemDto),2000L);
        Mockito.when(cartService.getItems(cartItemsCount)).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/cart/items")
                        .sessionAttr("cart",cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("cart"))
                .andExpect(MockMvcResultMatchers.model().attribute("items", expectedDto.items()));
    }

}