package com.microservice.quarkus.payment.application.port;

import com.microservice.quarkus.payment.domain.Money;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Port for Vendure Admin API operations.
 * Used to create products, variants, and assets in Vendure.
 */
public interface VendureAdminGateway {

  /**
   * Creates assets (images) in Vendure.
   * 
   * @param files     Input streams of the files to upload
   * @param fileNames Names of the files
   * @return List of created asset results
   */
  List<AssetResult> createAssets(List<InputStream> files, List<String> fileNames);

  /**
   * Creates a single asset (image) in Vendure.
   * 
   * @param file     Input stream of the file to upload
   * @param fileName Name of the file
   * @return Created asset result
   */
  AssetResult createAsset(InputStream file, String fileName);

  /**
   * Creates a product in Vendure.
   * 
   * @return the created product ID
   */
  String createProduct(CreateProductCommand command);

  /**
   * Creates variants for a product.
   * 
   * @return list of created variant IDs
   */
  List<String> createProductVariants(String productId, List<CreateVariantCommand> variants);

  /**
   * Finds a product by slug.
   */
  Optional<String> findProductIdBySlug(String slug);

  /**
   * Finds a variant by SKU.
   */
  Optional<String> findVariantIdBySku(String sku);

  /**
   * Finds an option group by code.
   */
  Optional<String> findOptionGroupIdByCode(String code);

  /**
   * Checks if Vendure Admin API is available.
   */
  boolean isAvailable();

  /**
   * Create Product's options
   */
  String createProductsOption(CreateProductsOptionCommand command);

  /**
   * Assign Product's options
   */
  String assignOptionGroupToProduct(AssignGroupToProductCommand command);

  /**
   * Command to create a product.
   */
  record CreateProductCommand(
      String name,
      String slug,
      String description,
      List<String> assetIds,
      String featuredAssetId) {
    public CreateProductCommand(String name, String slug, String description) {
      this(name, slug, description, List.of(), null);
    }
  }

  /**
   * Command to create a variant.
   */
  record CreateVariantCommand(
      String sku,
      String name,
      Money price,
      int stockOnHand) {
    public CreateVariantCommand(String sku, String name) {
      this(sku, name, Money.zero("USD"), 100);
    }

    public CreateVariantCommand(String sku, String name, Money price) {
      this(sku, name, price, 100);
    }
  }

  /**
   * Result of creating an asset.
   */
  record AssetResult(
      String id,
      String name,
      String source,
      String preview) {
  }

  /**
   * Command to create a Product's Option
   */
  record CreateProductsOptionCommand(
      String name,
      List<String> options) {
  }

  public record AssignGroupToProductCommand(
      String productId,
      String optionGroupId) {
  }
}
