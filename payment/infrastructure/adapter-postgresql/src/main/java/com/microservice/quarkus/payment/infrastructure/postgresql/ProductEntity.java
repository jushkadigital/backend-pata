package com.microservice.quarkus.payment.infrastructure.postgresql;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", schema = "quarkus")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "featured_asset_id")
    private String featuredAssetId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_assets", schema = "quarkus", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "asset_id")
    @OrderColumn(name = "position")
    private List<String> assetIds = new ArrayList<>();

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductVariantEntity> variants = new ArrayList<>();

    public void addVariant(ProductVariantEntity variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void clearVariants() {
        variants.forEach(v -> v.setProduct(null));
        variants.clear();
    }
}
