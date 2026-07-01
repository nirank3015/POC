package com.tdg.productcrud.controller;

import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<ProductResponseDto> productPage = productService.getAllProducts(pageable);
        return ResponseEntity.ok(productPage);
    }
}