package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.entity.Product;
import com.tdg.productcrud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductServiceImpl(productRepository);
    }

    @Test
    @DisplayName("should return a page of ProductResponseDto when valid Pageable is passed")
    void shouldReturnPageOfProducts_whenValidPageable() {
        // Arrange
        Product product = Product.builder()
            .id(1L)
            .name("Product A")
            .category("Category A")
            .price(19.99)
            .stockQuantity(50)
            .build();
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 1), 1);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        // Act
        Page<ProductResponseDto> result = productService.getAllProducts(PageRequest.of(0, 1));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Product A");
        verify(productRepository, times(1)).findAll(any(Pageable.class));
    }
}