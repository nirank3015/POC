package com.example.controller;

import com.example.dto.ProductResponseDto;
import com.example.service.ProductService;
import com.example.util.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("shouldReturnProducts_whenValidPageableProvided")
    void shouldReturnProducts_whenValidPageableProvided() throws Exception {
        ProductResponseDto product1 = ProductResponseDto.builder()
                .id(1L)
                .name("Product A")
                .category("Category A")
                .price(BigDecimal.valueOf(10.50))
                .stockQuantity(100)
                .createdAt(null)
                .build();

        ProductResponseDto product2 = ProductResponseDto.builder()
                .id(2L)
                .name("Product B")
                .category("Category B")
                .price(BigDecimal.valueOf(20.00))
                .stockQuantity(200)
                .createdAt(null)
                .build();

        Page<ProductResponseDto> productPage = new PageImpl<>(List.of(product1, product2), PageRequest.of(0, 20), 2);

        Mockito.when(productService.getAllProducts(Mockito.any())).thenReturn(productPage);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Products retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].name").value("Product A"))
                .andExpect(jsonPath("$.data.content[1].name").value("Product B"));
    }
}