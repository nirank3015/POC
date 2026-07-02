package com.tdg.productcrud.controller;

import com.tdg.productcrud.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("shouldReturnSwaggerUI_whenRequestingSwaggerUIEndpoint")
    void shouldReturnSwaggerUI_whenRequestingSwaggerUIEndpoint() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("shouldReturnApiDocs_whenRequestingApiDocsEndpoint")
    void shouldReturnApiDocs_whenRequestingApiDocsEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}