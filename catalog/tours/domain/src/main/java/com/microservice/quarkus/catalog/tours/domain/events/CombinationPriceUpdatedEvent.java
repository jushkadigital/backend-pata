package com.microservice.quarkus.catalog.tours.domain.events;

import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a combination's draft price is updated.
 * Payment module listens to this to update DraftPrice in Vendure.
 */
public record CombinationPriceUpdatedEvent(
    String tourId,
    String tourCode,
    String sku,
    String vendureVariantId,
    BigDecimal priceAmount,
    String priceCurrency,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public CombinationPriceUpdatedEvent(String tourId, String tourCode, String sku,
                                       String vendureVariantId, BigDecimal priceAmount,
                                       String priceCurrency) {
    this(tourId, tourCode, sku, vendureVariantId, priceAmount, priceCurrency,
         UUID.randomUUID(), Instant.now());
  }
}
