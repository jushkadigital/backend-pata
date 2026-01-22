package com.microservice.quarkus.catalog.tours.domain;

public record TourCode(String value) {

  public TourCode {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Tour code cannot be null or blank");
    }
    if (value.length() < 1 || value.length() > 37) {
      throw new IllegalArgumentException("Tour code must be between 1 and 37 characters");
    }
    if (!value.matches("^[a-zA-Z0-9_-]+$")) {
      throw new IllegalArgumentException("Tour code must contain only uppercase letters, numbers, underscores, scores");
    }
  }

  public static TourCode of(String value) {
    return new TourCode(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
