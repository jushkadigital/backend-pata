package com.microservice.quarkus.catalog.tours.domain;

import com.microservice.quarkus.catalog.tours.domain.service.ServiceConfiguration;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceSpec;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceType;

public record IncludedService(
    ServiceSpec service,
    int quantity,
    Integer durationHours
) {

    public IncludedService {
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (durationHours != null && (durationHours < 1 || durationHours > 24)) {
            throw new IllegalArgumentException("Duration must be between 1 and 24 hours");
        }
    }

    public static IncludedService of(ServiceSpec service, int quantity, Integer durationHours) {
        return new IncludedService(service, quantity, durationHours);
    }

    public static IncludedService of(ServiceSpec service, int quantity) {
        return new IncludedService(service, quantity, null);
    }

    public static IncludedService single(ServiceSpec service) {
        return new IncludedService(service, 1, null);
    }

    public String serviceName() {
        return service.name();
    }

    public ServiceType serviceType() {
        return service.type();
    }

    public boolean isMandatory() {
        return service.mandatory();
    }

    public ServiceConfiguration configuration() {
        return service.configuration();
    }
}
