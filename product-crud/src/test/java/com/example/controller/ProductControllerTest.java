package com.example.controller;

import com.example.dto.ProductResponseDto;
import com.example.service.ProductService;
import com.example.util.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("should return paginated products when valid query params are passed")
    void shouldReturnPaginatedProducts_whenValidQueryParamsPassed() throws Exception {
        ProductResponseDto product1 = ProductResponseDto.builder()
                .id(1L)
                .name("Product A")
                .price(19.99)
                .description("Description of Product A")
                .build();

        ProductResponseDto product2 = ProductResponseDto.builder()
                .id(2L)
                .name("Product B")
                .price(29.99)
                .description("Description of Product B")
                .build();

        Page<ProductResponseDto> productPage = new PageImpl<>(List.of(product1, product2),
                PageRequest.of(0, 2), 10);

        Mockito.when(productService.getAllProducts(Mockito.any()))
                .thenReturn(productPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products?page=0&size=2&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(10)))
                .andExpect(jsonPath("$.data.content[0].name", is("Product A")));
    }

    @Test
    @DisplayName("should use default pagination when no query params are provided")
    void shouldUseDefaultPagination_whenNoQueryParamsProvided() throws Exception {
        ProductResponseDto product1 = ProductResponseDto.builder()
                .id(1L)
                .name("Product A")
                .price(19.99)
                .description("Description of Product A")
                .build();

        Page<ProductResponseDto> productPage = new PageImpl<>(List.of(product1),
                PageRequest.of(0, 20), 1);

        Mockito.when(productService.getAllProducts(Mockito.any()))
                .thenReturn(productPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.number", is(0)))
                .andExpect(jsonPath("$.data.size", is(20)));
    }
}