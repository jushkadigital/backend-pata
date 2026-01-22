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
@Table(name = "tour_included_services", schema = "quarkus", indexes = {
        @Index(name = "idx_tour_included_services_tour", columnList = "tour_id")
})
@Getter
@Setter
public class IncludedServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private TourEntity tour;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory;

    @Column(name = "duration_hours")
    private Integer durationHours;
}
