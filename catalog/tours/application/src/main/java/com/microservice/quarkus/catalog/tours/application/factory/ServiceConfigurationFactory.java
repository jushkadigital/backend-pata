package com.microservice.quarkus.catalog.tours.application.factory;

import com.microservice.quarkus.catalog.tours.domain.service.FoodConfiguration;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceConfiguration;
import com.microservice.quarkus.catalog.tours.domain.service.ServiceType;
import com.microservice.quarkus.catalog.tours.domain.service.TicketConfiguration;
import com.microservice.quarkus.catalog.tours.domain.service.TransportConfiguration;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class ServiceConfigurationFactory {

    public ServiceConfiguration create(ServiceType serviceType, Map<String, Object> configMap) {
        if (configMap == null || configMap.isEmpty()) {
            return null;
        }

        return switch (serviceType) {
            case TICKET -> createTicketConfiguration(configMap);
            case TRANSPORT -> createTransportConfiguration(configMap);
            case FOOD -> createFoodConfiguration(configMap);
            default -> null;
        };
    }

    private TicketConfiguration createTicketConfiguration(Map<String, Object> config) {
        String seatType = getString(config, "seatType", "GENERAL");
        boolean includesGuide = getBoolean(config, "includesGuide", false);
        return new TicketConfiguration(seatType, includesGuide);
    }

    private TransportConfiguration createTransportConfiguration(Map<String, Object> config) {
        String vehicleType = getString(config, "vehicleType", "BUS");
        String vehicleClass = getString(config, "vehicleClass", "STANDARD");
        boolean includesDriver = getBoolean(config, "includesDriver", true);
        int capacity = getInt(config, "capacity", 1);
        return new TransportConfiguration(vehicleType, vehicleClass, includesDriver, capacity);
    }

    private FoodConfiguration createFoodConfiguration(Map<String, Object> config) {
        String mealType = getString(config, "mealType", "LUNCH");
        boolean includesDrinks = getBoolean(config, "includesDrinks", false);
        String dietaryRestrictions = getString(config, "dietaryRestrictions", null);
        return new FoodConfiguration(mealType, includesDrinks, dietaryRestrictions);
    }

    public Map<String, Object> toMap(ServiceConfiguration configuration) {
        if (configuration == null) {
            return null;
        }

        return switch (configuration) {
            case TicketConfiguration tc -> Map.of(
                    "seatType", tc.seatType(),
                    "includesGuide", tc.includesGuide()
            );
            case TransportConfiguration tc -> Map.of(
                    "vehicleType", tc.vehicleType(),
                    "vehicleClass", tc.vehicleClass(),
                    "includesDriver", tc.includesDriver(),
                    "capacity", tc.capacity()
            );
            case FoodConfiguration fc -> {
                if (fc.dietaryRestrictions() != null) {
                    yield Map.of(
                            "mealType", fc.mealType(),
                            "includesDrinks", fc.includesDrinks(),
                            "dietaryRestrictions", fc.dietaryRestrictions()
                    );
                } else {
                    yield Map.of(
                            "mealType", fc.mealType(),
                            "includesDrinks", fc.includesDrinks()
                    );
                }
            }
        };
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
