package com.tdg.productcrud.specification;

import com.tdg.productcrud.entity.Product;
import com.tdg.productcrud.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductSpecificationTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldFilterByCategory() {
        List<Product> products = productRepository.findAll(ProductSpecification.hasCategory("Electronics"));
        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(product -> "Electronics".equals(product.getCategory()));
    }

    @Test
    void shouldFilterByMinPrice() {
        BigDecimal minPrice = BigDecimal.valueOf(500);
        List<Product> products = productRepository.findAll(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice));
        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(product -> product.getPrice().compareTo(minPrice) >= 0);
    }

    @Test
    void shouldFilterByMaxPrice() {
        BigDecimal maxPrice = BigDecimal.valueOf(1500);
        List<Product> products = productRepository.findAll(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(product -> product.getPrice().compareTo(maxPrice) <= 0);
    }

    @Test
    void shouldFilterByCategoryAndPriceRanges() {
        BigDecimal minPrice = BigDecimal.valueOf(500);
        BigDecimal maxPrice = BigDecimal.valueOf(1500);
        List<Product> products = productRepository.findAll(
            ProductSpecification.hasCategory("Electronics")
                .and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice))
                .and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice))
        );
        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(product -> "Electronics".equals(product.getCategory()));
        assertThat(products).allMatch(product -> product.getPrice().compareTo(minPrice) >= 0);
        assertThat(products).allMatch(product -> product.getPrice().compareTo(maxPrice) <= 0);
    }
}