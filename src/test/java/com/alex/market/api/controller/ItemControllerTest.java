package com.alex.market.api.controller;

import com.alex.market.api.dto.ItemDto;
import com.alex.market.api.dto.PageDto;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.*;


@WebMvcTest(ItemController.class)
@ActiveProfiles("test")
class ItemControllerTest {

    @MockitoBean
    private ItemService itemService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getItems_shouldReturnViewAndModelWithData() throws Exception {
        Map<Long, Integer> cartItemsCount = Map.of(1L,1,2L,2,3L,3,4L,4);
        PageItemsDto expectedDto=new PageItemsDto(Collections.emptyList(),"test",SortColumn.PRICE.name(),new PageDto(3,1,false,false));

        Mockito.when(itemService.getItemsPage(Mockito.any(SearchDto.class))).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/items")
                .param("search","test")
                        .param("sort", SortColumn.NO.name())
                .param("pageNumber","1")
                .param("pageSize","3")
                 .sessionAttr("cart",cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("items"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(MockMvcResultMatchers.model().attributeExists("items","search","sort","paging"));
    }
    @Test
    void getItems_shouldSetParamsSomeDefaultValue_whenTheseParamsNotGiven() throws Exception {
        Map<Long, Integer> cartItemsCount = Map.of(1L,1,2L,2,3L,3,4L,4);
        PageItemsDto expectedDto=new PageItemsDto(Collections.emptyList(),"test",SortColumn.PRICE.name(),new PageDto(3,1,false,false));

        Mockito.when(itemService.getItemsPage(Mockito.any(SearchDto.class))).thenReturn(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/items")
                        .sessionAttr("cart",cartItemsCount))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("items"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(MockMvcResultMatchers.model().attributeExists("items","search","sort","paging"));
    }


}