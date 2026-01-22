package com.microservice.quarkus.catalog.tours.domain;

public record TourPolicy(
    PolicyType type,
    String description,
    String value) {

  public TourPolicy {
    if (type == null) {
      throw new IllegalArgumentException("Policy type cannot be null");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Policy description cannot be null or blank");
    }
  }

  public static TourPolicy cancellation(String description, String value) {
    return new TourPolicy(PolicyType.CANCELLATION, description, value);
  }

  public static TourPolicy refund(String description, String value) {
    return new TourPolicy(PolicyType.REFUND, description, value);
  }

  public static TourPolicy modification(String description, String value) {
    return new TourPolicy(PolicyType.MODIFICATION, description, value);
  }

  public static TourPolicy minPassengers(String description, int minPassengers) {
    return new TourPolicy(PolicyType.MIN_PASSENGERS, description, String.valueOf(minPassengers));
  }

  public static TourPolicy maxPassengers(String description, int maxPassengers) {
    return new TourPolicy(PolicyType.MAX_PASSENGERS, description, String.valueOf(maxPassengers));
  }

  public enum PolicyType {
    CANCELLATION,
    REFUND,
    MODIFICATION,
    MIN_PASSENGERS,
    MAX_PASSENGERS,
    AGE_RESTRICTION,
    HEALTH_REQUIREMENT,
    EQUIPMENT_REQUIRED,
    OTHER
  }
}
