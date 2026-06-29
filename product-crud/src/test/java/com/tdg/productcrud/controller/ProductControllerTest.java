package com.tdg.productcrud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.exception.GlobalExceptionHandler;
import com.tdg.productcrud.exception.ResourceNotFoundException;
import com.tdg.productcrud.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponseDto responseDto;
    private ProductRequestDto requestDto;

    @BeforeEach
    void setUp() {
        responseDto = new ProductResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Laptop");
        responseDto.setCategory("Electronics");
        responseDto.setPrice(new BigDecimal("999.99"));
        responseDto.setStockQuantity(50);
        responseDto.setCreatedAt(LocalDateTime.now());

        requestDto = new ProductRequestDto();
        requestDto.setName("Laptop");
        requestDto.setCategory("Electronics");
        requestDto.setPrice(new BigDecimal("999.99"));
        requestDto.setStockQuantity(50);
    }

    @Test
    void createProduct_returns201() throws Exception {
        when(productService.createProduct(any())).thenReturn(responseDto);
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Laptop"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllProducts_returns200WithList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(responseDto));
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void getProductById_found_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(responseDto);
        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getProductById_notFound_returns404() throws Exception {
        when(productService.getProductById(99L))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));
        mockMvc.perform(get("/api/products/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void updateProduct_returns200() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(responseDto);
        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void deleteProduct_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Product not found with id: 99"))
            .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }
}
