package com.tdg.productcrud.controller;

import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void testGetAllProducts() throws Exception {
        ProductResponseDto product = new ProductResponseDto();
        product.setId(1L);
        product.setName("Laptop");
        product.setCategory("Electronics");
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStockQuantity(10);

        List<ProductResponseDto> products = List.of(product);

        when(productService.getAllProducts()).thenReturn(products);

        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].category").value("Electronics"));

        verify(productService).getAllProducts();
    }

    @Test
    void testSearchProducts() throws Exception {
        ProductResponseDto product = new ProductResponseDto();
        product.setId(1L);
        product.setName("Smartphone");
        product.setCategory("Electronics");
        product.setPrice(BigDecimal.valueOf(800));
        product.setStockQuantity(20);

        List<ProductResponseDto> products = List.of(product);

        String category = "Electronics";
        BigDecimal minPrice = BigDecimal.valueOf(500);
        BigDecimal maxPrice = BigDecimal.valueOf(1500);

        when(productService.searchProducts(category, minPrice, maxPrice)).thenReturn(products);

        mockMvc.perform(get("/api/products")
                .param("category", category)
                .param("minPrice", "500")
                .param("maxPrice", "1500")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Smartphone"))
                .andExpect(jsonPath("$[0].category").value("Electronics"));

        verify(productService).searchProducts(category, minPrice, maxPrice);
    }
}