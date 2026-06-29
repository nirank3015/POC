package com.tdg.productcrud.specification;

import com.tdg.productcrud.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductSpecificationTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void testHasCategory() {
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("500"));
        product.setStockQuantity(10);
        entityManager.persist(product);

        Specification<Product> spec = ProductSpecification.hasCategory("Electronics");
        List<Product> results = entityManager.getEntityManager()
            .createQuery("SELECT p FROM Product p WHERE " + spec.toPredicate(), Product.class)
            .getResultList();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    void testHasPriceGreaterThanOrEqual() {
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("1000"));
        product.setStockQuantity(10);
        entityManager.persist(product);

        Specification<Product> spec = ProductSpecification.hasPriceGreaterThanOrEqual(new BigDecimal("500"));
        List<Product> results = entityManager.getEntityManager()
            .createQuery("SELECT p FROM Product p WHERE " + spec.toPredicate(), Product.class)
            .getResultList();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPrice()).isGreaterThanOrEqualTo(new BigDecimal("500"));
    }

    @Test
    void testHasPriceLessThanOrEqual() {
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("100"));
        product.setStockQuantity(10);
        entityManager.persist(product);

        Specification<Product> spec = ProductSpecification.hasPriceLessThanOrEqual(new BigDecimal("150"));
        List<Product> results = entityManager.getEntityManager()
            .createQuery("SELECT p FROM Product p WHERE " + spec.toPredicate(), Product.class)
            .getResultList();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPrice()).isLessThanOrEqualTo(new BigDecimal("150"));
    }
}