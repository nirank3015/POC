package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import java.util.List;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto requestDto);
    List<ProductResponseDto> getAllProducts();
    ProductResponseDto getProductById(Long id);
    ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto);
    void deleteProduct(Long id);
}
