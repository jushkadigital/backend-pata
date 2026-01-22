package com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tour_service_combinations", schema = "quarkus", indexes = {
        @Index(name = "idx_tour_service_combinations_tour", columnList = "tour_id")
})
@Getter
@Setter
public class ServiceCombinationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private TourEntity tour;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // Vendure integration
    @Column(name = "vendure_variant_id", length = 50)
    private String vendureVariantId;

    @Column(name = "price_amount", precision = 19, scale = 2)
    private java.math.BigDecimal priceAmount;

    @Column(name = "price_currency", length = 3)
    private String priceCurrency;

    @OneToMany(mappedBy = "combination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceCombinationItemEntity> items = new ArrayList<>();
}
