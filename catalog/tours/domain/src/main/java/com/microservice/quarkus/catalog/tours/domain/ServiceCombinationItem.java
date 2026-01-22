package com.microservice.quarkus.catalog.tours.domain;

/**
 * Value object representing a service reference within a combination.
 * Contains the service name and its order position within the combination.
 */
public record ServiceCombinationItem(
    String serviceName,
    int order
) {

    public ServiceCombinationItem {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be null or blank");
        }
        if (order < 0) {
            throw new IllegalArgumentException("Order must be non-negative");
        }
    }

    public static ServiceCombinationItem of(String serviceName, int order) {
        return new ServiceCombinationItem(serviceName, order);
    }
}
