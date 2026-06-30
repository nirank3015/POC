package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.entity.Product;
import com.tdg.productcrud.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        // Map Product entities to ProductResponseDto using a stream
        return productPage.map(this::toDto);
    }

    private ProductResponseDto toDto(Product product) {
        return ProductResponseDto.builder()
            .id(product.getId())
            .name(product.getName())
            .category(product.getCategory())
            .price(product.getPrice())
            .stockQuantity(product.getStockQuantity())
            .createdAt(product.getCreatedAt())
            .build();
    }
}