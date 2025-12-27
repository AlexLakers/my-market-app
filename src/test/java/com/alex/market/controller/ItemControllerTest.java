package com.alex.market.controller;

import com.alex.market.dto.input.CartChangeDto;
import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.dto.output.PageDto;
import com.alex.market.controller.ItemController;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.exception.TitleAlreadyExistsException;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.model.CartAction;
import com.alex.market.search.PageItemsDto;
import com.alex.market.search.SearchDto;
import com.alex.market.search.SortColumn;
import com.alex.market.service.ImageService;
import com.alex.market.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.servlet.FlashMap;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ItemController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ItemControllerTest {

    @MockitoBean(reset = MockReset.BEFORE)
    private ItemService itemService;
    @MockitoBean(reset = MockReset.BEFORE)
    private ImageService imageService;

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

        when(itemService.getItemsPage(any(SearchDto.class))).thenReturn(expectedDto);

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
        PageItemsDto expectedDto = new PageItemsDto(Collections.emptyList(), "test", SortColumn.PRICE.name(), new PageDto(3, 1, false, false));

        when(itemService.getItemsPage(any(SearchDto.class))).thenReturn(expectedDto);

        mockMvc.perform(get("/items")
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(model().attributeExists("items", "search", "sort", "paging"));
    }

    @Test
    void getItemById_shouldReturnViewAndModelWithDataSuccess() throws Exception {
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID));
        when(itemService.findByIdWithCartCount(VALID_ID, cartItemsCount)).thenReturn(expectedDto);

        mockMvc.perform(get("/items/{id}", VALID_ID)
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(model().attributeExists("item"));
    }

    @Test
    void getItemById_shouldThrowItemNotFoundException_whenItemNotFound() throws Exception {
        doThrow(new ItemNotFoundException(INVALID_ID)).when(itemService).findByIdWithCartCount(INVALID_ID, cartItemsCount);

        mockMvc.perform(get("/items/{id}", INVALID_ID)
                        .sessionAttr("cart", cartItemsCount))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeCartItemCountForItemsPage_shouldRedirectItemsPageWithAttrs() throws Exception {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID)+1);
        when(itemService.changeCartItemCount(givenDto)).thenReturn(expectedDto);

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
    public void changeCartItemCountForItemPage() throws Exception {
        CartChangeDto givenDto = new CartChangeDto(VALID_ID, CartAction.PLUS, cartItemsCount);
        ItemDto expectedDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, cartItemsCount.get(VALID_ID)+1);
        when(itemService.changeCartItemCount(givenDto)).thenReturn(expectedDto);

        mockMvc.perform(post("/items/{itemId}",VALID_ID)
                .sessionAttr("cart", cartItemsCount)
                .param("action", CartAction.PLUS.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(content().contentType(MediaType.valueOf("text/html;charset=UTF-8")))
                .andExpect(model().attributeExists("item"));
    }

    @Test
    void updateImageByItemId_shouldSetStatus302RedirectToItemPageSuccess() throws Exception {
        byte[] givenImage = new byte[]{(byte) 137, 80, 78, 71};
        MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpg", givenImage);
        doNothing().when(imageService).updateImageByItemId(file, VALID_ID);

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
        doThrow(ItemNotFoundException.class).when(imageService).updateImageByItemId(file, INVALID_ID);

        mockMvc.perform(multipart(HttpMethod.POST, "/admin/images/{id}", INVALID_ID)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_shouldSetStatus302RedirectToItemsPageAndThenReturnNewImagePageWithAttrAllFlowSuccess() throws Exception {
        ItemCreateDto givenDto = new ItemCreateDto("title", "desc", 1000L);
        ItemDto expectedDto = new ItemDto(VALID_ID, "title", "desc", null, 1000L, 1);
        when(itemService.createItem(givenDto)).thenReturn(expectedDto);

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
        ItemCreateDto givenDto = new ItemCreateDto("title", "desc", 1000L);
        doThrow(TitleAlreadyExistsException.class).when(itemService).createItem(givenDto);

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