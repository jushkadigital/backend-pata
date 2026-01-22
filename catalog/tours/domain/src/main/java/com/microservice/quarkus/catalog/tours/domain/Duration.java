package com.microservice.quarkus.catalog.tours.domain;

public record Duration(int hours) {

  public Duration {
    if (hours <= 0) {
      throw new IllegalArgumentException("Duration must be at least 1 hour");
    }
    if (hours > 24) {
      throw new IllegalArgumentException("Duration cannot exceed 24 hours");
    }
  }

  public static Duration of(int hours) {
    return new Duration(hours);
  }

  public static Duration halfDay() {
    return new Duration(4);
  }

  public static Duration fullDay() {
    return new Duration(8);
  }

  @Override
  public String toString() {
    if (hours == 1) {
      return "1 hour";
    }
    return hours + " hours";
  }
}
