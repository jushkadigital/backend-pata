package com.microservice.quarkus.payment.infrastructure.vendure.acl;

import com.microservice.quarkus.payment.application.port.VendureAdminGateway;
import com.microservice.quarkus.payment.infrastructure.vendure.client.VendureAdminClient;
import com.microservice.quarkus.payment.infrastructure.vendure.client.VendureAdminClient.VariantInput;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Anti-Corruption Layer for Vendure Admin API.
 * Implements VendureAdminGateway port.
 */
@ApplicationScoped
public class VendureAdminACL implements VendureAdminGateway {

  private static final Logger LOG = Logger.getLogger(VendureAdminACL.class);

  @Inject
  VendureAdminClient adminClient;

  @PostConstruct
  void init() {
    // Authenticate on startup
    if (adminClient.isAvailable()) {
      adminClient.authenticate();
    }
  }

  @Override
  public List<AssetResult> createAssets(List<InputStream> files, List<String> fileNames) {
    List<VendureAdminClient.AssetResult> clientResults = adminClient.createAssets(files, fileNames);
    return clientResults.stream()
        .map(r -> new AssetResult(r.id(), r.name(), r.source(), r.preview()))
        .toList();
  }

  @Override
  public AssetResult createAsset(InputStream file, String fileName) {
    return adminClient.createAsset(file, fileName)
        .map(r -> new AssetResult(r.id(), r.name(), r.source(), r.preview()))
        .orElseThrow(() -> new RuntimeException("Failed to upload asset: " + fileName));
  }

  @Override
  public String createProduct(CreateProductCommand command) {
    VendureAdminClient.ProductInput input = new VendureAdminClient.ProductInput(
        command.name(),
        command.slug(),
        command.description(),
        command.assetIds(),
        command.featuredAssetId());

    return adminClient.createProduct(input)
        .orElseThrow(() -> new RuntimeException("Failed to create product: " + command.name()));
  }

  @Override
  public List<String> createProductVariants(String productId, List<CreateVariantCommand> variants) {
    List<VariantInput> inputs = variants.stream()
        .map(v -> new VariantInput(
            v.sku(),
            v.name(),
            v.price().toMinorUnits(),
            v.stockOnHand()))
        .toList();

    return adminClient.createProductVariants(productId, inputs);
  }

  @Override
  public Optional<String> findProductIdBySlug(String slug) {
    return adminClient.findProductIdBySlug(slug);
  }

  @Override
  public Optional<String> findVariantIdBySku(String sku) {
    return adminClient.findVariantIdBySku(sku);
  }

  @Override
  public Optional<String> findOptionGroupIdByCode(String code) {
    return adminClient.findOptionGroupIdByCode(code);
  }

  @Override
  public boolean isAvailable() {
    return adminClient.isAvailable();
  }

  @Override
  public String createProductsOption(CreateProductsOptionCommand command) {
    VendureAdminClient.ProductsOptionInput input = new VendureAdminClient.ProductsOptionInput(
        command.name(),
        command.options());
    return adminClient.createProductsOption(input)
        .orElseThrow(() -> new RuntimeException("Failed to create product's option: " + command.name()));
  }

  @Override
  public String assignOptionGroupToProduct(AssignGroupToProductCommand command) {
    VendureAdminClient.OptionGroupToProductInput input = new VendureAdminClient.OptionGroupToProductInput(
        command.productId(),
        command.optionGroupId());
    return adminClient.assignOptionGroupToProduct(input)
        .orElseThrow(() -> new RuntimeException("Failed to assign product: " + command.productId()));
  }
}
