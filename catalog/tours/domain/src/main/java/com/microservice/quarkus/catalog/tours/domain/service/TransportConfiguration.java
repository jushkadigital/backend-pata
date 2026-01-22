package com.microservice.quarkus.catalog.tours.domain.service;

public record TransportConfiguration(
    String vehicleType,
    String vehicleClass,
    boolean includesDriver,
    int capacity
) implements ServiceConfiguration {

    public TransportConfiguration {
        if (vehicleType == null || vehicleType.isBlank()) {
            throw new IllegalArgumentException("Vehicle type cannot be null or blank");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
    }

    public static TransportConfiguration bus(String vehicleClass, int capacity) {
        return new TransportConfiguration("BUS", vehicleClass, true, capacity);
    }

    public static TransportConfiguration van(String vehicleClass, int capacity) {
        return new TransportConfiguration("VAN", vehicleClass, true, capacity);
    }

    public static TransportConfiguration car(String vehicleClass) {
        return new TransportConfiguration("CAR", vehicleClass, true, 4);
    }

    public static TransportConfiguration flight(String flightClass, int capacity) {
        return new TransportConfiguration("FLIGHT", flightClass, false, capacity);
    }

    public static TransportConfiguration train(String trainClass, int capacity) {
        return new TransportConfiguration("TRAIN", trainClass, false, capacity);
    }
}
