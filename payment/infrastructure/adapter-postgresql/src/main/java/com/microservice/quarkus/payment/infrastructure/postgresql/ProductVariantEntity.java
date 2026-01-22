package com.microservice.quarkus.payment.infrastructure.postgresql;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants", schema = "quarkus")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariantEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "name")
    private String name;

    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "stock_level", nullable = false)
    private int stockLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
}
