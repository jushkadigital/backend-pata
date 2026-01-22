package com.microservice.quarkus.catalog.tours.domain;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Value object representing a service combination (sellable package).
 * Each combination has a unique SKU for Vendure integration.
 */
public record ServiceCombination(
    String name,
    String description,
    String sku,
    List<ServiceCombinationItem> items,
    // Vendure integration
    String vendureVariantId,
    Money price
) {

    public ServiceCombination {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Combination name cannot be null or blank");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Combination must have at least one service");
        }
        items = List.copyOf(items);
    }

    /**
     * Creates a service combination from an ordered list of service names.
     */
    public static ServiceCombination of(String name, String description, String sku, List<String> serviceNames) {
        if (serviceNames == null || serviceNames.isEmpty()) {
            throw new IllegalArgumentException("Service names list cannot be null or empty");
        }

        List<ServiceCombinationItem> items = IntStream.range(0, serviceNames.size())
                .mapToObj(i -> ServiceCombinationItem.of(serviceNames.get(i), i))
                .toList();

        return new ServiceCombination(name, description, sku, items, null, null);
    }

    /**
     * Creates a service combination without description.
     */
    public static ServiceCombination of(String name, String sku, List<String> serviceNames) {
        return of(name, null, sku, serviceNames);
    }

    /**
     * Returns the service names in order.
     */
    public List<String> getServiceNames() {
        return items.stream()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .map(ServiceCombinationItem::serviceName)
                .toList();
    }

    /**
     * Returns the number of services in this combination.
     */
    public int size() {
        return items.size();
    }

    /**
     * Checks if this combination contains a specific service.
     */
    public boolean containsService(String serviceName) {
        return items.stream().anyMatch(item -> item.serviceName().equals(serviceName));
    }

    /**
     * Checks if this combination is linked to Vendure.
     */
    public boolean isLinkedToVendure() {
        return vendureVariantId != null && !vendureVariantId.isBlank();
    }

    /**
     * Checks if this combination has a price set.
     */
    public boolean hasPrice() {
        return price != null && price.isPositive();
    }

    /**
     * Returns a new combination with Vendure link.
     */
    public ServiceCombination withVendureLink(String vendureVariantId, Money price) {
        return new ServiceCombination(name, description, sku, items, vendureVariantId, price);
    }

    /**
     * Returns a new combination with updated price.
     */
    public ServiceCombination withPrice(Money price) {
        return new ServiceCombination(name, description, sku, items, vendureVariantId, price);
    }
}
