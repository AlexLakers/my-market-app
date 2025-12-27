package com.alex.market.controller;

import com.alex.market.dto.input.ItemCreateDto;
import com.alex.market.dto.output.ItemDto;
import com.alex.market.exception.ItemNotFoundException;
import com.alex.market.exception.TitleAlreadyExistsException;
import com.alex.market.exception.handler.GlobalExceptionHandler;
import com.alex.market.service.ImageService;
import com.alex.market.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import org.springframework.web.servlet.FlashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AdminControllerTest {
    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = 10000L;
    @MockitoBean(reset = MockReset.BEFORE)
    private ImageService imageService;
    @MockitoBean(reset = MockReset.BEFORE)
    private ItemService itemService;
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadImage_shouldSetStatus302RedirectToItemPageSuccess() throws Exception {
        byte[] givenImage = new byte[]{(byte) 137, 80, 78, 71};
        MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpg", givenImage);
        doNothing().when(imageService).updateImageByItemId(file, VALID_ID);

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, "/admin/images/{id}", VALID_ID)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + VALID_ID));
    }

    @Test
    void uploadImage_shouldSetStatus404WhenItemNotFoundFail() throws Exception {
        byte[] givenImage = new byte[]{(byte) 137, 80, 78, 71};
        MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpg", givenImage);
        doThrow(ItemNotFoundException.class).when(imageService).updateImageByItemId(file, INVALID_ID);

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, "/admin/images/{id}", INVALID_ID)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_shouldSetStatus302RedirectToItemsPageAndThenReturnNewImagePageWithAttrAllFlowSuccess() throws Exception {
        ItemCreateDto givenDto = new ItemCreateDto("title", "desc", 1000L);
        ItemDto expectedDto = new ItemDto(VALID_ID, "title", "desc", null, 1000L, 1);
        when(itemService.createItem(givenDto)).thenReturn(expectedDto);

        MvcResult result=mockMvc.perform(MockMvcRequestBuilders.post("/admin/items/add")
                .param("title", givenDto.title())
                .param("description", givenDto.description())
                .param("price", String.valueOf(givenDto.price()))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/images"))
                .andExpect(flash().attribute("item", expectedDto))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("item")).isEqualTo(expectedDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/images")
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("item", expectedDto))
                .andExpect(view().name("newImage"));
    }

    @Test
    void createItem_shouldSetStatus400_whenTitleAlreadyExistsFail() throws Exception {
        ItemCreateDto givenDto = new ItemCreateDto("title", "desc", 1000L);
        doThrow(TitleAlreadyExistsException.class).when(itemService).createItem(givenDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/items/add")
                        .param("title", givenDto.title())
                        .param("description", givenDto.description())
                        .param("price", String.valueOf(givenDto.price()))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void imagePage_shouldReturnViewNewImageSuccess() throws Exception {
        ItemDto flashItem = new ItemDto(1L, "Test", "Desc", null, 1000L, 0);

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/images")
                        .flashAttr("item", flashItem))
                .andExpect(status().isOk())
                .andExpect(model().attribute("item", flashItem))
                .andExpect(view().name("newImage"));
    }

    @Test
    void adminPage_shouldSet200AndReturnNewItemPageSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("newItem"));
    }
}