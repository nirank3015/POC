package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.entity.Product;
import com.tdg.productcrud.exception.ResourceNotFoundException;
import com.tdg.productcrud.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    @Override
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = toEntity(requestDto);
        return toDto(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toDto(product);
    }

    @Transactional
    @Override
    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setName(requestDto.getName());
        product.setCategory(requestDto.getCategory());
        product.setPrice(requestDto.getPrice());
        product.setStockQuantity(requestDto.getStockQuantity());
        return toDto(productRepository.save(product));
    }

    @Transactional
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private Product toEntity(ProductRequestDto dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setCategory(dto.getCategory());
        p.setPrice(dto.getPrice());
        p.setStockQuantity(dto.getStockQuantity());
        return p;
    }

    private ProductResponseDto toDto(Product p) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCategory(p.getCategory());
        dto.setPrice(p.getPrice());
        dto.setStockQuantity(p.getStockQuantity());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
