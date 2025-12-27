package com.alex.market.integration.controller;

import com.alex.market.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class OrderControllerIT  extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1000L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        cartItemsCount.put(2L, 3);
    }

    @Test
    void createOrder_shouldRedirectToOrdersById() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/buy")
                .sessionAttr("cart",cartItemsCount))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/" + 1004 + "?newOrder=true"));
    }

    @Test
    void getOrders_shouldExistsModelAndView() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("orders"))
                .andExpect(view().name("orders"));
    }

    @Test
    void getOrder_shouldReturnOneDtoAndView() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}",VALID_ID)
                        .param("newOrder","false"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("order"))
                .andExpect(view().name("order"));
    }
    @Test
    void getOrder_shouldSetStatus404_whenNotFoundFail() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}",INVALID_ID)
                        .param("newOrder","false"))
                .andExpect(status().isNotFound());
    }
}