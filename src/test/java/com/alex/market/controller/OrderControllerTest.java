package com.alex.market.controller;

import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.OrderDto;
import com.alex.market.controller.OrderController;
import com.alex.market.exception.OrderNotFoundException;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.service.OrderService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(reset = MockReset.BEFORE)
    private OrderService orderService;

    private Map<Long, Integer> cartItemsCount;
    private ItemDto itemDto;

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        cartItemsCount.put(2L, 3);
        itemDto=new ItemDto(1L, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
    }

    @Test
    void createOrder_shouldRedirectToOrdersById() throws Exception {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        when(orderService.createOrder(cartItemsCount)).thenReturn(orderDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/buy")
                .sessionAttr("cart",cartItemsCount))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/" + VALID_ID + "?newOrder=true"));
    }

    @Test
    void getOrders_shouldExistsModelAndView() throws Exception {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        when(orderService.getOrders()).thenReturn(List.of(orderDto));

        mockMvc.perform(MockMvcRequestBuilders.get("/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("orders"))
                .andExpect(view().name("orders"));
    }

    @Test
    void getOrder_shouldReturnOneDtoAndView() throws Exception {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto),1000L);
        when(orderService.getOrder(VALID_ID)).thenReturn(orderDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}",VALID_ID)
                        .param("newOrder","false"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("order"))
                .andExpect(view().name("order"));
    }
    @Test
    void getOrder_shouldSetStatus404_whenNotFoundFail() throws Exception {
        doThrow(OrderNotFoundException.class).when(orderService).getOrder(INVALID_ID);

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}",INVALID_ID)
                        .param("newOrder","false"))
                .andExpect(status().isNotFound());
    }
}