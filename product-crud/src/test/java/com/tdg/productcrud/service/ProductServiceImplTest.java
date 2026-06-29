package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.entity.Product;
import com.tdg.productcrud.exception.ResourceNotFoundException;
import com.tdg.productcrud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequestDto requestDto;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(50);
        product.setCreatedAt(LocalDateTime.now());

        requestDto = new ProductRequestDto();
        requestDto.setName("Laptop");
        requestDto.setCategory("Electronics");
        requestDto.setPrice(new BigDecimal("999.99"));
        requestDto.setStockQuantity(50);
    }

    @Test
    void createProduct_returnsResponseDto() {
        when(productRepository.save(any(Product.class))).thenReturn(product);
        ProductResponseDto result = productService.createProduct(requestDto);
        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getCategory()).isEqualTo("Electronics");
        assertThat(result.getPrice()).isEqualByComparingTo("999.99");
    }

    @Test
    void getAllProducts_returnsList() {
        when(productRepository.findAll()).thenReturn(List.of(product));
        List<ProductResponseDto> result = productService.getAllProducts();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void getProductById_found_returnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        ProductResponseDto result = productService.getProductById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProductById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void updateProduct_found_updatesAndReturns() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        ProductResponseDto result = productService.updateProduct(1L, requestDto);
        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.updateProduct(99L, requestDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteProduct_found_deletesSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        productService.deleteProduct(1L);
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.deleteProduct(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
