package com.microservice.quarkus.catalog.tours.domain;

import java.util.UUID;

public record TourId(String value) {

  public TourId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Tour ID cannot be null or blank");
    }
  }

  public static TourId newId() {
    return new TourId(UUID.randomUUID().toString());
  }

  public static TourId of(String value) {
    return new TourId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
