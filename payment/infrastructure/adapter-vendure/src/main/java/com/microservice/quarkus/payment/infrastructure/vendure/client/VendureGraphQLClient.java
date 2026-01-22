package com.microservice.quarkus.payment.infrastructure.vendure.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.payment.infrastructure.vendure.dto.VendureProductDTO;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GraphQL client for Vendure Shop API.
 * Uses the same HttpClient approach as VendureAdminClient.
 */
@ApplicationScoped
public class VendureGraphQLClient {

    @ConfigProperty(name = "vendure.url", defaultValue = "http://localhost:3000")
    String vendureBaseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public VendureGraphQLClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Gets the Shop API URL.
     */
    private String getShopApiUrl() {
        return vendureBaseUrl + "/shop-api";
    }

    /**
     * Fetches all products from Vendure.
     */
    public List<VendureProductDTO> fetchAllProducts() {
        String query = """
            query {
                products {
                    items {
                        id
                        name
                        slug
                        description
                        variants {
                            id
                            sku
                            name
                            priceWithTax
                            currencyCode
                            stockLevel
                        }
                    }
                }
            }
            """;

        JsonNode response = executeQuery(query, Map.of());
        if (response == null) {
            return List.of();
        }

        JsonNode items = response.path("products").path("items");
        return objectMapper.convertValue(items, new TypeReference<List<VendureProductDTO>>() {});
    }

    /**
     * Fetches products with pagination.
     */
    public List<VendureProductDTO> fetchProducts(int skip, int take) {
        String query = """
            query($options: ProductListOptions) {
                products(options: $options) {
                    items {
                        id
                        name
                        slug
                        description
                        variants {
                            id
                            sku
                            name
                            priceWithTax
                            currencyCode
                            stockLevel
                        }
                    }
                }
            }
            """;

        Map<String, Object> variables = Map.of(
            "options", Map.of("skip", skip, "take", take)
        );

        JsonNode response = executeQuery(query, variables);
        if (response == null) {
            return List.of();
        }

        JsonNode items = response.path("products").path("items");
        return objectMapper.convertValue(items, new TypeReference<List<VendureProductDTO>>() {});
    }

    /**
     * Fetches a product by ID.
     */
    public Optional<VendureProductDTO> fetchProductById(String id) {
        String query = """
            query($id: ID!) {
                product(id: $id) {
                    id
                    name
                    slug
                    description
                    variants {
                        id
                        sku
                        name
                        priceWithTax
                        currencyCode
                        stockLevel
                    }
                }
            }
            """;

        JsonNode response = executeQuery(query, Map.of("id", id));
        if (response == null) {
            return Optional.empty();
        }

        JsonNode productNode = response.path("product");
        if (productNode.isNull() || productNode.isMissingNode()) {
            return Optional.empty();
        }

        return Optional.of(objectMapper.convertValue(productNode, VendureProductDTO.class));
    }

    /**
     * Fetches a product by slug.
     */
    public Optional<VendureProductDTO> fetchProductBySlug(String slug) {
        String query = """
            query($slug: String!) {
                product(slug: $slug) {
                    id
                    name
                    slug
                    description
                    variants {
                        id
                        sku
                        name
                        priceWithTax
                        currencyCode
                        stockLevel
                    }
                }
            }
            """;

        JsonNode response = executeQuery(query, Map.of("slug", slug));
        if (response == null) {
            return Optional.empty();
        }

        JsonNode productNode = response.path("product");
        if (productNode.isNull() || productNode.isMissingNode()) {
            return Optional.empty();
        }

        return Optional.of(objectMapper.convertValue(productNode, VendureProductDTO.class));
    }

    /**
     * Checks if Vendure Shop API is available.
     */
    public boolean isAvailable() {
        try {
            String query = "query { __typename }";
            JsonNode response = executeQuery(query, Map.of());
            return response != null;
        } catch (Exception e) {
            System.out.println("Vendure Shop API health check failed: " + e.getMessage());
            return false;
        }
    }

    private JsonNode executeQuery(String query, Map<String, Object> variables) {
        try {
            JsonObject body = new JsonObject()
                    .put("query", query)
                    .put("variables", new JsonObject(objectMapper.writeValueAsString(variables)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getShopApiUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("errors")) {
                System.err.println("GraphQL errors: " + root.get("errors"));
            }

            if (root.has("data")) {
                return root.get("data");
            }

            return null;
        } catch (Exception e) {
            System.err.println("GraphQL request failed: " + e.getMessage());
            return null;
        }
    }
}
