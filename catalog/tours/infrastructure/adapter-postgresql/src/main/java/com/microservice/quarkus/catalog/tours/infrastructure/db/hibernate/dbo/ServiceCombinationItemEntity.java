package com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tour_service_combination_items", schema = "quarkus", indexes = {
        @Index(name = "idx_tour_service_combination_items_combination", columnList = "combination_id")
})
@Getter
@Setter
public class ServiceCombinationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combination_id", nullable = false)
    private ServiceCombinationEntity combination;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;
}
