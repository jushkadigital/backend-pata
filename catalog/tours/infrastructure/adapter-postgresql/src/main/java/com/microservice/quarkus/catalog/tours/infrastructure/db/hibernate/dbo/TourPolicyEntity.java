package com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo;

import com.microservice.quarkus.catalog.tours.domain.TourPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tour_policies", schema = "quarkus", indexes = {
        @Index(name = "idx_tour_policies_tour", columnList = "tour_id"),
        @Index(name = "idx_tour_policies_type", columnList = "policy_type")
})
@Getter
@Setter
public class TourPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private TourEntity tour;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private TourPolicy.PolicyType policyType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "policy_value")
    private String value;
}
