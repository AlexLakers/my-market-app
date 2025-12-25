package com.alex.market.api.controller;

import com.alex.market.api.dto.input.CartChangeDto;
import com.alex.market.api.dto.output.ItemDto;
import com.alex.market.api.dto.output.PageDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.model.CartAction;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.*;


@WebMvcTest(ItemController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ItemControllerTest {

    @MockitoBean(reset = MockReset.BEFORE)
    private ItemService itemService;

    @Autowired
    private MockMvc mockMvc;

    private Map<Long, Integer> cartItemsCount;

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = Long.MAX_VALUE;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 2);
        cartItemsCount.put(2L, 3);
    }

    @Test
    void getItems_shouldReturnViewAndModelWithData() throws Exception {
        PageItemsDto expectedDto = new PageItemsDto(Collections.emptyList(), "test", SortColumn.PRICE.name(), new PageDto(3, 1, false, false));

        Mockito.when(itemService.getItemsPage(Mockito.any(SearchDto.class))).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/items")
                        .param("search", "test")
                        .param("sort", SortColumn.NO.name())
                        .param("pageNumber", "1")
                        .param("pageSize", "3")
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("items"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(MockMvcResultMatchers.model().attributeExists("items", "search", "sort", "paging"));
    }

    @Test
    void getItems_shouldSetParamsSomeDefaultValue_whenTheseParamsNotGiven() throws Exception {
        PageItemsDto expectedDto = new PageItemsDto(Collections.emptyList(), "test", SortColumn.PRICE.name(), new PageDto(3, 1, false, false));

        Mockito.when(itemService.getItemsPage(Mockito.any(SearchDto.class))).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/items")
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("items"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(MockMvcResultMatchers.model().attributeExists("items", "search", "sort", "paging"));
    }

    @Test
    void getItemById_shouldReturnViewAndModelWithDataSuccess() throws Exception {
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID));
        Mockito.when(itemService.findByIdWithCartCount(VALID_ID, cartItemsCount)).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/items/{id}", VALID_ID)
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("item"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(MockMvcResultMatchers.model().attributeExists("item"));
    }

    @Test
    void getItemById_shouldThrowItemNotFoundException_whenItemNotFound() throws Exception {
        Mockito.doThrow(new ItemNotFoundException(INVALID_ID)).when(itemService).findByIdWithCartCount(INVALID_ID, cartItemsCount);

        mockMvc.perform(MockMvcRequestBuilders.get("/items/{id}", INVALID_ID)
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void changeCartItemCountForItemsPage_shouldRedirectItemsPageWithAttrs() throws Exception {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID)+1);
        Mockito.when(itemService.changeCartItemCount(givenDto)).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/items")
                .param("id", VALID_ID.toString())
                .param("action", CartAction.PLUS.name())
                .param("search", "test")
                .param("sort", SortColumn.NO.name())
                .param("pageNumber", "1")
                .param("pageSize", "3")
                .sessionAttr("cart", cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/items?search=test&sort=NO&pageSize=3&pageNumber=1"))
                .andExpect(MockMvcResultMatchers.view().name("redirect:/items"));

    }
    @Test
    public void changeCartItemCountForItemPage() throws Exception {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID)+1);
        Mockito.when(itemService.changeCartItemCount(givenDto)).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/items/{itemId}",VALID_ID)
                .sessionAttr("cart", cartItemsCount)
                .param("action", CartAction.PLUS.name()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("item"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(MockMvcResultMatchers.model().attributeExists("item"));
    }


}