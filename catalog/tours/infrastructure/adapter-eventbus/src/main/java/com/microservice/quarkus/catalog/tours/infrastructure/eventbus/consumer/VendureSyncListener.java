package com.microservice.quarkus.catalog.tours.infrastructure.eventbus.consumer;

import com.microservice.quarkus.catalog.tours.domain.Money;
import com.microservice.quarkus.catalog.tours.domain.Tour;
import com.microservice.quarkus.catalog.tours.domain.TourCode;
import com.microservice.quarkus.catalog.tours.domain.TourRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.jbosslog.JBossLog;

/**
 * Listens for Vendure sync events from payment module.
 * Updates Tour's Vendure references when products are synced.
 */
@JBossLog
@ApplicationScoped
public class VendureSyncListener {

    private static final String VENDURE_PRODUCT_SYNCED = "payment.vendure.product-synced";

    private final TourRepository tourRepository;

    @Inject
    public VendureSyncListener(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    @ConsumeEvent(VENDURE_PRODUCT_SYNCED)
    @Blocking
    @Transactional
    public void onVendureProductSynced(String eventPayload) {
        log.infof("Received VendureProductSynced event: %s", eventPayload);

        try {
            JsonObject event = new JsonObject(eventPayload);

            String tourCode = event.getString("tourCode");
            String vendureProductId = event.getString("vendureProductId");
            JsonArray variants = event.getJsonArray("variants");

            tourRepository.findLatestVersionByCode(TourCode.of(tourCode))
                    .ifPresentOrElse(
                            tour -> updateTourWithVendureLinks(tour, vendureProductId, variants),
                            () -> log.warnf("Tour not found for code: %s", tourCode)
                    );

        } catch (Exception e) {
            log.errorf(e, "Error processing VendureProductSynced event: %s", eventPayload);
        }
    }

    private void updateTourWithVendureLinks(Tour tour, String vendureProductId, JsonArray variants) {
        // Link tour to Vendure product
        if (!tour.isLinkedToVendure()) {
            tour.linkToVendureProduct(vendureProductId);
            log.infof("Linked tour %s to Vendure product %s", tour.getCode().value(), vendureProductId);
        }

        // Link combinations to Vendure variants
        if (variants != null) {
            for (int i = 0; i < variants.size(); i++) {
                JsonObject variant = variants.getJsonObject(i);
                String sku = variant.getString("sku");
                String vendureVariantId = variant.getString("vendureVariantId");
                long priceInCents = variant.getLong("priceInCents", 0L);
                String currency = variant.getString("currency", "USD");

                try {
                    Money price = Money.fromMinorUnits(priceInCents, currency);
                    tour.linkCombinationToVendure(sku, vendureVariantId, price);
                    log.infof("Linked combination %s to Vendure variant %s with price %s",
                            sku, vendureVariantId, price);
                } catch (IllegalArgumentException e) {
                    log.warnf("Could not link combination %s: %s", sku, e.getMessage());
                }
            }
        }

        // Persist the updated tour
        tourRepository.update(tour);
        log.infof("Updated tour %s with Vendure references", tour.getCode().value());
    }
}
