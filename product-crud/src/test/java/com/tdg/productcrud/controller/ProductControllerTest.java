package com.tdg.productcrud.controller;

import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Test
    @DisplayName("should return paginated products when valid query parameters are provided")
    void shouldReturnPaginatedProducts_whenValidQueryParamsPassed() throws Exception {
        // Arrange
        ProductResponseDto productResponseDto = ProductResponseDto.builder()
            .id(1L)
            .name("Product A")
            .price(19.99)
            .stockQuantity(50)
            .build();

        Page<ProductResponseDto> productPage = new PageImpl<>(List.of(productResponseDto), PageRequest.of(0, 1), 1);
        when(productService.getAllProducts(any())).thenReturn(productPage);

        // Act and Assert
        mockMvc.perform(get("/api/products?page=0&size=1&sort=id,asc"))
            .andDo(print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].name").value("Product A"));
    }
}