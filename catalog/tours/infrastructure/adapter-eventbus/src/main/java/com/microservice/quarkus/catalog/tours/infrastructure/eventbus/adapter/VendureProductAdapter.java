package com.microservice.quarkus.catalog.tours.infrastructure.eventbus.adapter;

import com.microservice.quarkus.catalog.tours.application.port.VendureProductPort;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Adapter that implements VendureProductPort using EventBus request-reply.
 * Communicates synchronously with payment module to create Vendure products/variants.
 */
@ApplicationScoped
public class VendureProductAdapter implements VendureProductPort {

    private static final String VENDURE_CREATE_PRODUCT = "payment.vendure.create-product";
    private static final String VENDURE_CREATE_VARIANT = "payment.vendure.create-variant";
    private static final String VENDURE_CREATE_SERVICE = "payment.vendure.create-service";
    private static final String VENDURE_GET_SERVICE_PRICE = "payment.vendure.get-service-price";
    private static final long TIMEOUT_SECONDS = 30;

    @Inject
    EventBus eventBus;

    @Override
    public String createProduct(CreateProductRequest request) {
        System.out.println("Requesting Vendure product creation for: " + request.slug());

        JsonObject payload = new JsonObject()
                .put("name", request.name())
                .put("slug", request.slug())
                .put("description", request.description())
                .put("enabled", request.enabled())
                .put("draftName", request.draftName())
                .put("draftSlug", request.draftSlug())
                .put("draftDescription", request.draftDescription())
                .put("draftEnabled", request.draftEnabled());

        try {
            CompletableFuture<String> future = new CompletableFuture<>();

            eventBus.<String>request(VENDURE_CREATE_PRODUCT, payload.encode())
                    .onSuccess(reply -> future.complete(reply.body()))
                    .onFailure(future::completeExceptionally);

            String responseStr = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            JsonObject response = new JsonObject(responseStr);

            if (response.getBoolean("success", false)) {
                String productId = response.getString("productId");
                System.out.println("Vendure product created: " + productId);
                return productId;
            } else {
                String error = response.getString("error", "Unknown error");
                throw new VendureOperationException("Failed to create product: " + error);
            }

        } catch (VendureOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new VendureOperationException("Error communicating with payment module: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> createVariants(CreateVariantsRequest request) {
        System.out.println("Requesting Vendure variant creation for product: " + request.vendureProductId());

        JsonArray variantsArray = new JsonArray();
        for (VariantData v : request.variants()) {
            JsonObject variant = new JsonObject()
                    .put("sku", v.sku())
                    .put("name", v.name())
                    .put("priceInCents", v.priceInCents())
                    .put("stockOnHand", v.stockOnHand());

            if (v.draftSku() != null) {
                variant.put("draftSku", v.draftSku());
            }
            if (v.draftPriceInCents() != null) {
                variant.put("draftPriceInCents", v.draftPriceInCents());
            }
            if (v.draftStockOnHand() != null) {
                variant.put("draftStockOnHand", v.draftStockOnHand());
            }

            variantsArray.add(variant);
        }

        JsonObject payload = new JsonObject()
                .put("productId", request.vendureProductId())
                .put("variants", variantsArray);

        try {
            CompletableFuture<String> future = new CompletableFuture<>();

            eventBus.<String>request(VENDURE_CREATE_VARIANT, payload.encode())
                    .onSuccess(reply -> future.complete(reply.body()))
                    .onFailure(future::completeExceptionally);

            String responseStr = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            JsonObject response = new JsonObject(responseStr);

            if (response.getBoolean("success", false)) {
                Map<String, String> result = new HashMap<>();
                JsonArray variants = response.getJsonArray("variants");

                for (int i = 0; i < variants.size(); i++) {
                    JsonObject v = variants.getJsonObject(i);
                    result.put(v.getString("sku"), v.getString("variantId"));
                }

                System.out.println("Created " + result.size() + " Vendure variants");
                return result;
            } else {
                String error = response.getString("error", "Unknown error");
                throw new VendureOperationException("Failed to create variants: " + error);
            }

        } catch (VendureOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new VendureOperationException("Error communicating with payment module: " + e.getMessage(), e);
        }
    }

    @Override
    public ServiceVariantResult createServiceVariant(CreateServiceRequest request) {
        System.out.println("Requesting Vendure service creation for: " + request.serviceName() + " (" + request.serviceType() + ")");

        JsonObject payload = new JsonObject()
                .put("serviceType", request.serviceType())
                .put("serviceName", request.serviceName())
                .put("sku", request.sku())
                .put("draftPriceInCents", request.draftPriceInCents());

        try {
            CompletableFuture<String> future = new CompletableFuture<>();

            eventBus.<String>request(VENDURE_CREATE_SERVICE, payload.encode())
                    .onSuccess(reply -> future.complete(reply.body()))
                    .onFailure(future::completeExceptionally);

            String responseStr = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            JsonObject response = new JsonObject(responseStr);

            if (response.getBoolean("success", false)) {
                String productId = response.getString("serviceProductId");
                String variantId = response.getString("variantId");
                System.out.println("Vendure service created: productId=" + productId + ", variantId=" + variantId);
                return new ServiceVariantResult(productId, variantId);
            } else {
                String error = response.getString("error", "Unknown error");
                throw new VendureOperationException("Failed to create service: " + error);
            }

        } catch (VendureOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new VendureOperationException("Error communicating with payment module: " + e.getMessage(), e);
        }
    }

    @Override
    public long getServicePrice(String sku) {
        System.out.println("Requesting Vendure service price for SKU: " + sku);

        JsonObject payload = new JsonObject().put("sku", sku);

        try {
            CompletableFuture<String> future = new CompletableFuture<>();

            eventBus.<String>request(VENDURE_GET_SERVICE_PRICE, payload.encode())
                    .onSuccess(reply -> future.complete(reply.body()))
                    .onFailure(future::completeExceptionally);

            String responseStr = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            JsonObject response = new JsonObject(responseStr);

            if (response.getBoolean("success", false)) {
                long price = response.getLong("priceInCents", 0L);
                System.out.println("Service price for " + sku + ": " + price);
                return price;
            } else {
                System.out.println("Could not get price for " + sku + ", returning 0");
                return 0L;
            }

        } catch (Exception e) {
            System.out.println("Error getting service price: " + e.getMessage() + ", returning 0");
            return 0L;
        }
    }
}
