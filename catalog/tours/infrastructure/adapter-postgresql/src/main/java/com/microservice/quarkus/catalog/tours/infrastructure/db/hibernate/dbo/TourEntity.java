package com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo;

import com.microservice.quarkus.catalog.tours.domain.TourStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tours", schema = "quarkus", indexes = {
        @Index(name = "idx_tours_code", columnList = "code"),
        @Index(name = "idx_tours_code_version", columnList = "code, version", unique = true),
        @Index(name = "idx_tours_status", columnList = "status"),
        @Index(name = "idx_tours_parent", columnList = "parent_tour_id")
})
@Getter
@Setter
public class TourEntity {

    @Id
    private String id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "duration_hours", nullable = false)
    private int durationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TourStatus status;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "parent_tour_id")
    private String parentTourId;

    // Vendure integration
    @Column(name = "vendure_product_id", length = 50)
    private String vendureProductId;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IncludedServiceEntity> includedServices = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TourPolicyEntity> policies = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceCombinationEntity> serviceCombinations = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
