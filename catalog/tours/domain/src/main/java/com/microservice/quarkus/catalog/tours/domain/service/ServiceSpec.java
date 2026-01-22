package com.microservice.quarkus.catalog.tours.domain.service;

/**
 * Specification of a service that can be included in a tour.
 * Prices are managed in Vendure, not here.
 */
public record ServiceSpec(
    ServiceType type,
    String name,
    boolean mandatory,
    ServiceConfiguration configuration
) {

    public ServiceSpec {
        if (type == null) {
            throw new IllegalArgumentException("Service type cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be null or blank");
        }
        validateConfigurationMatchesType(type, configuration);
    }

    private static void validateConfigurationMatchesType(ServiceType type, ServiceConfiguration configuration) {
        if (configuration == null) {
            return;
        }

        boolean valid = switch (type) {
            case TICKET -> configuration instanceof TicketConfiguration;
            case FOOD -> configuration instanceof FoodConfiguration;
            case TRANSPORT -> configuration instanceof TransportConfiguration;
            default -> true;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                "Configuration type does not match service type: " + type
            );
        }
    }

    public static ServiceSpec mandatory(ServiceType type, String name, ServiceConfiguration config) {
        return new ServiceSpec(type, name, true, config);
    }

    public static ServiceSpec optional(ServiceType type, String name, ServiceConfiguration config) {
        return new ServiceSpec(type, name, false, config);
    }

    public static ServiceSpec mandatory(ServiceType type, String name) {
        return new ServiceSpec(type, name, true, null);
    }

    public static ServiceSpec optional(ServiceType type, String name) {
        return new ServiceSpec(type, name, false, null);
    }

    public boolean isTicket() {
        return type == ServiceType.TICKET;
    }

    public boolean isFood() {
        return type == ServiceType.FOOD;
    }

    public boolean isTransport() {
        return type == ServiceType.TRANSPORT;
    }

    public boolean hasConfiguration() {
        return configuration != null;
    }

    public <T extends ServiceConfiguration> T getConfigurationAs(Class<T> clazz) {
        if (configuration == null) {
            return null;
        }
        if (!clazz.isInstance(configuration)) {
            throw new IllegalStateException(
                "Configuration is not of type " + clazz.getSimpleName()
            );
        }
        return clazz.cast(configuration);
    }
}
