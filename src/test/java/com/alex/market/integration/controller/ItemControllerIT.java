package com.alex.market.integration.controller;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.model.CartAction;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ImageService;
import com.alex.market.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.FlashMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;


public class ItemControllerIT extends BaseIntegrationTest {

    @Autowired
    private ItemService itemService;
    @Autowired
    private ImageService imageService;

    @Autowired
    private MockMvc mockMvc;

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
    void getItems_shouldReturnViewAndModelWithData() throws Exception {
        PageItemsDto expectedDto = new PageItemsDto(Collections.emptyList(), "test", SortColumn.PRICE.name(), new PageDto(3, 1, false, false));

        mockMvc.perform(get("/items")
                        .param("search", "test")
                        .param("sort", SortColumn.NO.name())
                        .param("pageNumber", "1")
                        .param("pageSize", "3")
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(model().attributeExists("items", "search", "sort", "paging"));
    }

    @Test
    void getItems_shouldSetParamsSomeDefaultValue_whenTheseParamsNotGiven() throws Exception {

        mockMvc.perform(get("/items")
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(model().attributeExists("items", "sort", "paging"));
    }

    @Test
    void changeCartItemCountForItemsPage_shouldRedirectItemsPageWithAttrs() throws Exception {

        mockMvc.perform(post("/items")
                        .param("id", VALID_ID.toString())
                        .param("action", CartAction.PLUS.name())
                        .param("search", "test")
                        .param("sort", SortColumn.NO.name())
                        .param("pageNumber", "1")
                        .param("pageSize", "3")
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=test&sort=NO&pageSize=3&pageNumber=1"))
                .andExpect(view().name("redirect:/items"));

    }

    @Test
    void updateImageByItemId_shouldSetStatus302RedirectToItemPageSuccess() throws Exception {
        byte[] givenImage = new byte[]{(byte) 137, 80, 78, 71};
        MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpg", givenImage);

        mockMvc.perform(multipart(HttpMethod.POST, "/items/{id}/images/new", VALID_ID)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + VALID_ID));
    }

    @Test
    void updateImageByItemId_shouldSetStatus404WhenItemNotFoundFail() throws Exception {
        byte[] givenImage = new byte[]{(byte) 137, 80, 78, 71};
        MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpg", givenImage);
        //doThrow(ItemNotFoundException.class).when(imageService).updateImageByItemId(file, INVALID_ID);

        mockMvc.perform(multipart(HttpMethod.POST, "/items/{id}/images/new", INVALID_ID)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_shouldSetStatus302RedirectToItemsPageAndThenReturnNewImagePageWithAttrAllFlowSuccess() throws Exception {
        ItemCreateDto givenDto = new ItemCreateDto("title", "desc", 1000L);
        ItemDto expectedDto = new ItemDto(1004L, "title", "desc", null, 1000L, 0);

        MvcResult result=mockMvc.perform(post("/items/new")
                        .param("title", givenDto.title())
                        .param("description", givenDto.description())
                        .param("price", String.valueOf(givenDto.price()))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/images/new"))
                .andExpect(flash().attribute("item", expectedDto))
                .andReturn();


        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("item")).isEqualTo(expectedDto);

        mockMvc.perform(get("/items/images/new")
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("item", expectedDto))
                .andExpect(view().name("newImage"));
    }

    @Test
    void createItem_shouldSetStatus400_whenTitleAlreadyExistsFail() throws Exception {
        ItemCreateDto givenDto = new ItemCreateDto("test-ball", "desc", 1000L);

        mockMvc.perform(post("/items/new")
                        .param("title", givenDto.title())
                        .param("description", givenDto.description())
                        .param("price", String.valueOf(givenDto.price()))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void showNewImage_shouldReturnViewNewImageSuccess() throws Exception {
        ItemDto flashItem = new ItemDto(1L, "Test", "Desc", null, 1000L, 0);

        mockMvc.perform(get("/items/images/new")
                        .flashAttr("item", flashItem))
                .andExpect(status().isOk())
                .andExpect(model().attribute("item", flashItem))
                .andExpect(view().name("newImage"));
    }

    @Test
    void showNewItem_shouldSet200AndReturnNewItemPageSuccess() throws Exception {
        mockMvc.perform(get("/items/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("newItem"));
    }

}
