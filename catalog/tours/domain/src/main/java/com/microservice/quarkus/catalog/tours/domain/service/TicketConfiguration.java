package com.microservice.quarkus.catalog.tours.domain.service;

public record TicketConfiguration(
    String seatType,
    boolean includesGuide
) implements ServiceConfiguration {

    public TicketConfiguration {
        if (seatType == null || seatType.isBlank()) {
            throw new IllegalArgumentException("Seat type cannot be null or blank");
        }
    }

    public static TicketConfiguration withGuide(String seatType) {
        return new TicketConfiguration(seatType, true);
    }

    public static TicketConfiguration withoutGuide(String seatType) {
        return new TicketConfiguration(seatType, false);
    }
}
