package com.tdg.productcrud.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ProductEntityTest {

    @Test
    void prePersist_setsCreatedAt() {
        Product product = new Product();
        product.setName("Laptop");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(10);

        product.onCreate();

        assertThat(product.getCreatedAt()).isNotNull();
    }
}
