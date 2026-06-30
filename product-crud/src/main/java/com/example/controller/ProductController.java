package com.example.controller;

import com.example.dto.ProductResponseDto;
import com.example.service.ProductService;
import com.example.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ProductResponseDto>> getAllProducts(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "sort", required = false) String sort) {

        Page<ProductResponseDto> products = productService.getAllProducts(pageable);
        return new ApiResponse<>("Products retrieved successfully", products);
    }
}