package com.microservice.quarkus.payment.infrastructure.vendure.client;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for Vendure Admin GraphQL API.
 * Supports creating products, variants, and assets.
 */
@ApplicationScoped
public class VendureAdminClient {

  private static final Logger LOG = Logger.getLogger(VendureAdminClient.class);

  @ConfigProperty(name = "vendure.url", defaultValue = "http://localhost:3001")
  String vendureBaseUrl;

  @ConfigProperty(name = "vendure.admin.username", defaultValue = "superadmin")
  String username;

  @ConfigProperty(name = "vendure.admin.password", defaultValue = "superadmin")
  String password;

  private final HttpClient httpClient;
  private String authToken;

  @Inject
  public VendureAdminClient() {
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  /**
   * Authenticates with Vendure and stores the token.
   */
  public boolean authenticate() {
    String query = """
        mutation Login($username: String!, $password: String!) {
            login(username: $username, password: $password) {
                ... on CurrentUser {
                    id
                    identifier
                }
                ... on InvalidCredentialsError {
                    message
                }
            }
        }
        """;

    JsonObject variables = new JsonObject()
        .put("username", username)
        .put("password", password);

    try {
      JsonObject response = executeGraphQL(query, variables, false);
      if (response.containsKey("data")) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("login") && data.getJsonObject("login").containsKey("id")) {
          LOG.info("Successfully authenticated with Vendure Admin API");
          return true;
        }
      }
      LOG.warn("Failed to authenticate with Vendure Admin API");
      return false;
    } catch (Exception e) {
      LOG.errorf(e, "Error authenticating with Vendure Admin API");
      return false;
    }
  }

  /**
   * Creates a single asset (image) in Vendure via multipart upload.
   */
  public Optional<AssetResult> createAsset(InputStream file, String fileName) {
    List<AssetResult> results = createAssets(List.of(file), List.of(fileName));
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }

  /**
   * Creates multiple assets (images) in Vendure via multipart upload.
   */
  public List<AssetResult> createAssets(List<InputStream> files, List<String> fileNames) {
    if (files.size() != fileNames.size()) {
      throw new IllegalArgumentException("Files and fileNames lists must have the same size");
    }

    String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");
    List<AssetResult> results = new ArrayList<>();

    try {
      // Build multipart body
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      // Add operations part (GraphQL mutation)
      JsonArray mapArray = new JsonArray();
      for (int i = 0; i < files.size(); i++) {
        mapArray.add(new JsonObject().put("file", (Object) null));
      }

      JsonObject operations = new JsonObject()
          .put("query", """
              mutation CreateAssets($input: [CreateAssetInput!]!) {
                  createAssets(input: $input) {
                      ... on Asset {
                          id
                          name
                          source
                          preview
                      }
                      ... on MimeTypeError {
                          message
                      }
                  }
              }
              """)
          .put("variables", new JsonObject().put("input", mapArray));

      appendMultipartField(bos, boundary, "operations", operations.encode());

      // Add map part (file mapping)
      JsonObject map = new JsonObject();
      for (int i = 0; i < files.size(); i++) {
        map.put(String.valueOf(i), new JsonArray().add("variables.input." + i + ".file"));
      }
      appendMultipartField(bos, boundary, "map", map.encode());

      // Add file parts
      for (int i = 0; i < files.size(); i++) {
        appendMultipartFile(bos, boundary, String.valueOf(i), fileNames.get(i), files.get(i));
      }

      // End boundary
      bos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(getAdminApiUrl()))
          .timeout(Duration.ofSeconds(60))
          .header("Content-Type", "multipart/form-data; boundary=" + boundary)
          .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()));

      if (authToken != null) {
        requestBuilder.header("Authorization", "Bearer " + authToken);
      }

      HttpResponse<String> response = httpClient.send(requestBuilder.build(),
          HttpResponse.BodyHandlers.ofString());

      // Extract session token from cookies
      extractSessionToken(response);

      JsonObject jsonResponse = new JsonObject(response.body());

      if (jsonResponse.containsKey("errors")) {
        LOG.errorf("GraphQL errors creating assets: %s", jsonResponse.getJsonArray("errors").encode());
        return results;
      }

      if (jsonResponse.containsKey("data") && jsonResponse.getValue("data") != null) {
        JsonObject data = jsonResponse.getJsonObject("data");
        if (data.containsKey("createAssets") && data.getValue("createAssets") != null) {
          JsonArray assets = data.getJsonArray("createAssets");
          for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.getJsonObject(i);
            if (asset.containsKey("id")) {
              results.add(new AssetResult(
                  asset.getString("id"),
                  asset.getString("name"),
                  asset.getString("source"),
                  asset.getString("preview")));
            } else if (asset.containsKey("message")) {
              LOG.warnf("MimeTypeError for asset: %s", asset.getString("message"));
            }
          }
        }
      }

      LOG.infof("Created %d assets", results.size());
      return results;

    } catch (Exception e) {
      LOG.errorf(e, "Error creating assets");
      return results;
    }
  }

  private void appendMultipartField(ByteArrayOutputStream bos, String boundary,
      String name, String value) throws Exception {
    String field = "--" + boundary + "\r\n" +
        "Content-Disposition: form-data; name=\"" + name + "\"\r\n" +
        "Content-Type: application/json\r\n\r\n" +
        value + "\r\n";
    bos.write(field.getBytes(StandardCharsets.UTF_8));
  }

  private void appendMultipartFile(ByteArrayOutputStream bos, String boundary,
      String name, String fileName, InputStream file) throws Exception {
    String header = "--" + boundary + "\r\n" +
        "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n" +
        "Content-Type: application/octet-stream\r\n\r\n";
    bos.write(header.getBytes(StandardCharsets.UTF_8));
    file.transferTo(bos);
    bos.write("\r\n".getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a product in Vendure.
   */
  public Optional<String> createProduct(ProductInput input) {
    String query = """
        mutation CreateProduct($input: CreateProductInput!) {
            createProduct(input: $input) {
                id
                name
                slug
                featuredAsset {
                    id
                    preview
                }
            }
        }
        """;

    JsonObject translation = new JsonObject()
        .put("languageCode", "en")
        .put("name", input.name())
        .put("slug", input.slug())
        .put("description", input.description() != null ? input.description() : "");

    JsonObject graphqlInput = new JsonObject()
        .put("translations", new JsonArray().add(translation))
        .put("enabled", true);

    // Add asset IDs if provided
    if (input.assetIds() != null && !input.assetIds().isEmpty()) {
      JsonArray assetIds = new JsonArray();
      input.assetIds().forEach(assetIds::add);
      graphqlInput.put("assetIds", assetIds);
    }

    // Add featured asset ID if provided
    if (input.featuredAssetId() != null && !input.featuredAssetId().isBlank()) {
      graphqlInput.put("featuredAssetId", input.featuredAssetId());
    }

    JsonObject variables = new JsonObject().put("input", graphqlInput);

    try {
      JsonObject response = executeGraphQL(query, variables, true);
      if (response.containsKey("data")) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("createProduct")) {
          String productId = data.getJsonObject("createProduct").getString("id");
          LOG.infof("Created product %s with slug %s", productId, input.slug());
          return Optional.of(productId);
        }
      }
      if (response.containsKey("errors")) {
        LOG.errorf("Failed to create product: %s", response.getJsonArray("errors").encode());
      }
      return Optional.empty();
    } catch (Exception e) {
      LOG.errorf(e, "Error creating product %s", input.name());
      return Optional.empty();
    }
  }

  /**
   * Creates variants for a product.
   */
  public List<String> createProductVariants(String productId, List<VariantInput> variants) {
    String query = """
        mutation CreateProductVariants($input: [CreateProductVariantInput!]!) {
            createProductVariants(input: $input) {
                id
                sku
                name
                price
            }
        }
        """;

    JsonArray inputArray = new JsonArray();
    for (VariantInput variant : variants) {
      JsonObject translation = new JsonObject()
          .put("languageCode", "en")
          .put("name", variant.name());

      JsonObject variantInput = new JsonObject()
          .put("productId", productId)
          .put("sku", variant.sku())
          .put("translations", new JsonArray().add(translation))
          .put("price", variant.priceInCents())
          .put("stockOnHand", variant.stockOnHand());

      inputArray.add(variantInput);
    }

    JsonObject variables = new JsonObject().put("input", inputArray);

    LOG.infof("Creating %d variant(s) for product %s", variants.size(), productId);

    try {
      JsonObject response = executeGraphQL(query, variables, true);
      List<String> createdIds = new ArrayList<>();

      if (response.containsKey("errors")) {
        JsonArray errors = response.getJsonArray("errors");
        LOG.errorf("GraphQL errors creating variants: %s", errors.encode());
      }

      if (response.containsKey("data") && response.getValue("data") != null) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("createProductVariants") && data.getValue("createProductVariants") != null) {
          JsonArray created = data.getJsonArray("createProductVariants");
          if (created != null) {
            for (int i = 0; i < created.size(); i++) {
              JsonObject variant = created.getJsonObject(i);
              if (variant != null && variant.containsKey("id")) {
                createdIds.add(variant.getString("id"));
              }
            }
            LOG.infof("Created %d variants for product %s", createdIds.size(), productId);
          }
        }
      }

      return createdIds;
    } catch (Exception e) {
      LOG.errorf(e, "Error creating variants for product %s", productId);
      return List.of();
    }
  }

  /**
   * Finds a product by slug.
   */
  public Optional<String> findProductIdBySlug(String slug) {
    String query = """
        query GetProductBySlug($slug: String!) {
            product(slug: $slug) {
                id
            }
        }
        """;

    JsonObject variables = new JsonObject().put("slug", slug);

    try {
      JsonObject response = executeGraphQL(query, variables, true);
      if (response.containsKey("data")) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("product") && data.getValue("product") != null) {
          JsonObject product = data.getJsonObject("product");
          if (product != null) {
            return Optional.ofNullable(product.getString("id"));
          }
        }
      }
      return Optional.empty();
    } catch (Exception e) {
      LOG.errorf(e, "Error finding product by slug %s", slug);
      return Optional.empty();
    }
  }

  /**
   * Finds a variant by SKU.
   */
  public Optional<String> findVariantIdBySku(String sku) {
    String query = """
        query GetVariantBySku($options: ProductVariantListOptions) {
            productVariants(options: $options) {
                items {
                    id
                    sku
                }
            }
        }
        """;

    JsonObject filterBy = new JsonObject()
        .put("sku", new JsonObject().put("eq", sku));
    JsonObject options = new JsonObject()
        .put("filter", filterBy)
        .put("take", 1);
    JsonObject variables = new JsonObject().put("options", options);

    try {
      JsonObject response = executeGraphQL(query, variables, true);
      if (response.containsKey("data") && response.getValue("data") != null) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("productVariants") && data.getValue("productVariants") != null) {
          JsonObject productVariants = data.getJsonObject("productVariants");
          if (productVariants.containsKey("items") && productVariants.getValue("items") != null) {
            JsonArray items = productVariants.getJsonArray("items");
            if (items != null && !items.isEmpty()) {
              JsonObject variant = items.getJsonObject(0);
              if (variant != null && variant.containsKey("id")) {
                return Optional.of(variant.getString("id"));
              }
            }
          }
        }
      }
      return Optional.empty();
    } catch (Exception e) {
      LOG.errorf(e, "Error finding variant by SKU %s", sku);
      return Optional.empty();
    }
  }

  /**
   * Finds an OptionGroup by its code.
   */
  public Optional<String> findOptionGroupIdByCode(String code) {
    String query = """
        query GetOptionGroups($options: ProductOptionGroupListOptions) {
          productOptionGroups(options: $options) {
            items {
              id
              code
            }
          }
        }
        """;

    JsonObject filterBy = new JsonObject()
        .put("code", new JsonObject().put("eq", code));
    JsonObject options = new JsonObject()
        .put("filter", filterBy)
        .put("take", 1);
    JsonObject variables = new JsonObject().put("options", options);

    try {
      JsonObject response = executeGraphQL(query, variables, true);
      if (response.containsKey("data") && response.getValue("data") != null) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("productOptionGroups") && data.getValue("productOptionGroups") != null) {
          JsonObject productOptionGroups = data.getJsonObject("productOptionGroups");
          if (productOptionGroups.containsKey("items") && productOptionGroups.getValue("items") != null) {
            JsonArray items = productOptionGroups.getJsonArray("items");
            if (items != null && !items.isEmpty()) {
              JsonObject optionGroup = items.getJsonObject(0);
              if (optionGroup != null && optionGroup.containsKey("id")) {
                String id = optionGroup.getString("id");
                LOG.infof("Found optionGroup with code '%s' -> id: %s", code, id);
                return Optional.of(id);
              }
            }
          }
        }
      }
      LOG.warnf("OptionGroup with code '%s' not found", code);
      return Optional.empty();
    } catch (Exception e) {
      LOG.errorf(e, "Error finding optionGroup by code %s", code);
      return Optional.empty();
    }
  }

  /**
   * Checks if Vendure is available using the /health endpoint.
   */
  public boolean isAvailable() {
    String healthUrl = vendureBaseUrl + "/health";
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(healthUrl))
          .timeout(Duration.ofSeconds(5))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        LOG.debugf("Vendure health response: %s", response.body());
        return true;
      }

      LOG.warnf("Vendure health check failed with status: %d", response.statusCode());
      return false;
    } catch (Exception e) {
      LOG.warnf("Vendure health check failed: %s", e.getMessage());
      return false;
    }
  }

  private String getAdminApiUrl() {
    return vendureBaseUrl + "/admin-api";
  }

  private JsonObject executeGraphQL(String query, JsonObject variables, boolean withAuth) throws Exception {
    JsonObject body = new JsonObject()
        .put("query", query)
        .put("variables", variables);

    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(getAdminApiUrl()))
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body.encode()));

    if (withAuth && authToken != null) {
      requestBuilder.header("Authorization", "Bearer " + authToken);
    }

    HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

    extractSessionToken(response);

    return new JsonObject(response.body());
  }

  private void extractSessionToken(HttpResponse<?> response) {
    response.headers().allValues("set-cookie").stream()
        .filter(cookie -> cookie.startsWith("session="))
        .findFirst()
        .ifPresent(cookie -> {
          String token = cookie.split(";")[0].substring("session=".length());
          this.authToken = token;
        });
  }

  /**
   * Creates a ProductOptionGroup with its options in Vendure.
   * Following the correct pattern: 1) Create parent group, 2) Create child options.
   */
  public Optional<String> createProductsOption(ProductsOptionInput input) {
    // Step 1: Create the parent OptionGroup (without options)
    String createGroupQuery = """
        mutation CreateOptionGroup($input: CreateProductOptionGroupInput!) {
          createProductOptionGroup(input: $input) {
            id
            code
          }
        }
        """;

    JsonObject translation = new JsonObject()
        .put("languageCode", "en")
        .put("name", input.name());

    JsonObject groupInput = new JsonObject()
        .put("code", input.name())
        .put("translations", new JsonArray().add(translation))
        .put("options", new JsonArray());

    JsonObject variables = new JsonObject().put("input", groupInput);

    try {
      LOG.infof("Creating OptionGroup: %s", input.name());
      JsonObject response = executeGraphQL(createGroupQuery, variables, true);

      if (response.containsKey("errors")) {
        LOG.errorf("Failed to create option group: %s", response.getJsonArray("errors").encode());
        return Optional.empty();
      }

      if (!response.containsKey("data") || response.getValue("data") == null) {
        LOG.error("No data in response when creating option group");
        return Optional.empty();
      }

      JsonObject data = response.getJsonObject("data");
      if (!data.containsKey("createProductOptionGroup") || data.getValue("createProductOptionGroup") == null) {
        LOG.error("createProductOptionGroup returned null");
        return Optional.empty();
      }

      String optionGroupId = data.getJsonObject("createProductOptionGroup").getString("id");
      LOG.infof("Created OptionGroup with ID: %s", optionGroupId);

      // Step 2: Create each child option pointing to the parent
      for (String optionCode : input.options()) {
        createProductOption(optionGroupId, optionCode);
      }

      LOG.infof("Successfully created OptionGroup %s with %d options",
          optionGroupId, input.options().size());
      return Optional.of(optionGroupId);

    } catch (Exception e) {
      LOG.errorf(e, "Error creating option group %s", input.name());
      return Optional.empty();
    }
  }

  /**
   * Creates a single ProductOption within an OptionGroup.
   */
  private Optional<String> createProductOption(String optionGroupId, String code) {
    String query = """
        mutation CreateProductOption($input: CreateProductOptionInput!) {
          createProductOption(input: $input) {
            id
            code
            name
          }
        }
        """;

    JsonObject translation = new JsonObject()
        .put("languageCode", "en")
        .put("name", code);

    JsonObject optionInput = new JsonObject()
        .put("productOptionGroupId", optionGroupId)
        .put("code", code)
        .put("translations", new JsonArray().add(translation));

    JsonObject variables = new JsonObject().put("input", optionInput);

    try {
      LOG.infof("Creating ProductOption '%s' in group %s", code, optionGroupId);
      JsonObject response = executeGraphQL(query, variables, true);

      if (response.containsKey("errors")) {
        LOG.errorf("Failed to create product option: %s", response.getJsonArray("errors").encode());
        return Optional.empty();
      }

      if (response.containsKey("data") && response.getValue("data") != null) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("createProductOption") && data.getValue("createProductOption") != null) {
          String optionId = data.getJsonObject("createProductOption").getString("id");
          LOG.infof("Created ProductOption '%s' with ID: %s", code, optionId);
          return Optional.of(optionId);
        }
      }

      LOG.warnf("No data returned when creating product option '%s'", code);
      return Optional.empty();
    } catch (Exception e) {
      LOG.errorf(e, "Error creating product option '%s'", code);
      return Optional.empty();
    }
  }

  /**
   * Assigns an OptionGroup to a Product.
   * First checks if already assigned, then assigns if not.
   */
  public Optional<String> assignOptionGroupToProduct(OptionGroupToProductInput input) {
    // First, check if the OptionGroup is already assigned to this product
    if (isOptionGroupAssignedToProduct(input.productId(), input.optionGroupId())) {
      LOG.infof("OptionGroup %s is already assigned to product %s - skipping",
          input.optionGroupId(), input.productId());
      return Optional.of(input.productId());
    }

    String query = """
        mutation AddOptionGroupToProduct($productId: ID!, $optionGroupId: ID!) {
          addOptionGroupToProduct(
            productId: $productId
            optionGroupId: $optionGroupId
          ) {
            id
            optionGroups {
              id
              code
            }
          }
        }
        """;

    JsonObject variables = new JsonObject()
        .put("productId", input.productId())
        .put("optionGroupId", input.optionGroupId());

    LOG.infof("Assigning optionGroupId %s to productId %s", input.optionGroupId(), input.productId());

    try {
      JsonObject response = executeGraphQL(query, variables, true);
      LOG.debugf("assignOptionGroupToProduct response: %s", response.encode());

      if (response.containsKey("errors")) {
        JsonArray errors = response.getJsonArray("errors");
        String errorMessage = errors.encode();

        // Check if error is "already assigned" - treat as success
        if (errorMessage.contains("already assigned")) {
          LOG.infof("OptionGroup %s is already assigned to product %s - OK",
              input.optionGroupId(), input.productId());
          return Optional.of(input.productId());
        }

        LOG.errorf("Failed to assign option group to product: %s", errorMessage);
        return Optional.empty();
      }

      if (response.containsKey("data") && response.getValue("data") != null) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("addOptionGroupToProduct") && data.getValue("addOptionGroupToProduct") != null) {
          JsonObject product = data.getJsonObject("addOptionGroupToProduct");
          String productId = product.getString("id");

          // Log assigned option groups for verification
          if (product.containsKey("optionGroups") && product.getValue("optionGroups") != null) {
            JsonArray optionGroups = product.getJsonArray("optionGroups");
            LOG.infof("Product %s now has %d option group(s): %s",
                productId, optionGroups.size(), optionGroups.encode());
          }

          LOG.infof("Successfully assigned optionGroup %s to product %s",
              input.optionGroupId(), productId);
          return Optional.of(productId);
        }
      }

      LOG.warnf("No data returned when assigning option group %s to product %s",
          input.optionGroupId(), input.productId());
      return Optional.empty();
    } catch (Exception e) {
      LOG.errorf(e, "Error assigning option group %s to product %s",
          input.optionGroupId(), input.productId());
      return Optional.empty();
    }
  }

  /**
   * Checks if an OptionGroup is already assigned to a Product.
   */
  private boolean isOptionGroupAssignedToProduct(String productId, String optionGroupId) {
    String query = """
        query GetProduct($id: ID!) {
          product(id: $id) {
            id
            optionGroups {
              id
            }
          }
        }
        """;

    JsonObject variables = new JsonObject().put("id", productId);

    try {
      JsonObject response = executeGraphQL(query, variables, true);

      if (response.containsKey("data") && response.getValue("data") != null) {
        JsonObject data = response.getJsonObject("data");
        if (data.containsKey("product") && data.getValue("product") != null) {
          JsonObject product = data.getJsonObject("product");
          if (product.containsKey("optionGroups") && product.getValue("optionGroups") != null) {
            JsonArray optionGroups = product.getJsonArray("optionGroups");
            for (int i = 0; i < optionGroups.size(); i++) {
              JsonObject og = optionGroups.getJsonObject(i);
              if (og != null && optionGroupId.equals(og.getString("id"))) {
                return true;
              }
            }
          }
        }
      }
      return false;
    } catch (Exception e) {
      LOG.warnf("Error checking if option group is assigned: %s", e.getMessage());
      return false;
    }
  }

  /**
   * Input for creating a product.
   */
  public record ProductInput(
      String name,
      String slug,
      String description,
      List<String> assetIds,
      String featuredAssetId) {
    public ProductInput(String name, String slug, String description) {
      this(name, slug, description, List.of(), null);
    }
  }

  /**
   * Input for creating a variant.
   */
  public record VariantInput(
      String sku,
      String name,
      long priceInCents,
      int stockOnHand) {
    public VariantInput(String sku, String name) {
      this(sku, name, 0, 100);
    }

    public VariantInput(String sku, String name, long priceInCents) {
      this(sku, name, priceInCents, 100);
    }
  }

  /**
   * Result of creating an asset.
   */
  public record AssetResult(
      String id,
      String name,
      String source,
      String preview) {
  }

  /**
   * Input to create a Product's Option
   */
  public record ProductsOptionInput(
      String name,
      List<String> options) {
  }

  public record OptionGroupToProductInput(
      String productId,
      String optionGroupId) {
  }
}
