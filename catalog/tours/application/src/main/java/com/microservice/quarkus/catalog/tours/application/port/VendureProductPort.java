package com.microservice.quarkus.catalog.tours.application.port;

import java.util.List;
import java.util.Map;

/**
 * Port for creating products/variants in Vendure through payment module.
 * This allows catalog to get Vendure IDs before creating Tours.
 */
public interface VendureProductPort {

    /**
     * Creates a product in Vendure for a Tour.
     *
     * @param request the product creation request
     * @return the created product ID
     * @throws VendureOperationException if creation fails
     */
    String createProduct(CreateProductRequest request);

    /**
     * Creates variants in Vendure for ServiceCombinations.
     *
     * @param request the variant creation request
     * @return map of SKU to vendureVariantId
     * @throws VendureOperationException if creation fails
     */
    Map<String, String> createVariants(CreateVariantsRequest request);

    /**
     * Creates a service variant in Vendure.
     * Services are stored as ProductVariants under a Product per service type.
     *
     * @param request the service creation request
     * @return the service variant info (productId and variantId)
     * @throws VendureOperationException if creation fails
     */
    ServiceVariantResult createServiceVariant(CreateServiceRequest request);

    /**
     * Gets the price of a service variant by its SKU.
     *
     * @param sku the service SKU
     * @return the price in cents, or 0 if not found
     */
    long getServicePrice(String sku);

    /**
     * Request to create a product in Vendure.
     */
    record CreateProductRequest(
            String name,
            String slug,
            String description,
            boolean enabled,
            // Draft fields
            String draftName,
            String draftSlug,
            String draftDescription,
            boolean draftEnabled
    ) {
        public CreateProductRequest(String name, String slug, String description) {
            this(name, slug, description, true, null, null, null, false);
        }

        /**
         * Creates a draft-only product (not enabled, all data in draft fields).
         */
        public static CreateProductRequest draft(String name, String slug, String description) {
            return new CreateProductRequest(
                    name, slug, description,
                    false,  // not enabled yet
                    name, slug, description,  // draft fields same as main
                    true   // draft enabled
            );
        }
    }

    /**
     * Request to create variants in Vendure.
     */
    record CreateVariantsRequest(
            String vendureProductId,
            List<VariantData> variants
    ) {}

    /**
     * Data for a single variant.
     */
    record VariantData(
            String sku,
            String name,
            long priceInCents,
            int stockOnHand,
            // Draft fields
            String draftSku,
            Long draftPriceInCents,
            Integer draftStockOnHand
    ) {
        public VariantData(String sku, String name) {
            this(sku, name, 0, 100, null, null, null);
        }

        /**
         * Creates a draft variant with price only in draft field.
         */
        public static VariantData draft(String sku, String name, long draftPriceInCents) {
            return new VariantData(
                    sku, name,
                    0, 100,  // published price is 0
                    sku, draftPriceInCents, 100
            );
        }
    }

    /**
     * Request to create a service variant in Vendure.
     * Services are grouped by type into Products.
     */
    record CreateServiceRequest(
            String serviceType,    // TRANSPORT, ACCOMMODATION, etc.
            String serviceName,    // "Bus Lima-Cusco"
            String sku,            // "SVC-BUS-LIMA-CUSCO"
            long draftPriceInCents // initial draft price (usually 0)
    ) {
        public static CreateServiceRequest of(String serviceType, String serviceName, String sku) {
            return new CreateServiceRequest(serviceType, serviceName, sku, 0L);
        }
    }

    /**
     * Result of creating a service variant.
     */
    record ServiceVariantResult(
            String serviceProductId,  // Product ID for the service type
            String variantId          // Variant ID for this specific service
    ) {}

    /**
     * Exception thrown when Vendure operation fails.
     */
    class VendureOperationException extends RuntimeException {
        public VendureOperationException(String message) {
            super(message);
        }

        public VendureOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
