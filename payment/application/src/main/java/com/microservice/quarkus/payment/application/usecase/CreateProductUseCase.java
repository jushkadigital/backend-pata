package com.microservice.quarkus.payment.application.usecase;

import com.microservice.quarkus.payment.application.port.VendureAdminGateway;
import com.microservice.quarkus.payment.application.port.VendureAdminGateway.AssignGroupToProductCommand;
import com.microservice.quarkus.payment.application.port.VendureAdminGateway.CreateProductCommand;
import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import java.util.List;

/**
 * Use case for creating a product in Vendure and storing it locally.
 */
@ApplicationScoped
public class CreateProductUseCase {

  private static final Logger LOG = Logger.getLogger(CreateProductUseCase.class);

  @Inject
  VendureAdminGateway vendureGateway;

  @Inject
  ProductRepository productRepository;

  /**
   * Creates a product in Vendure and saves it locally.
   *
   * @param command The create product command
   * @return Result of the creation
   */
  @Transactional
  public CreateProductResult execute(CreateProductCommand command) {
    try {
      LOG.infof("Creating product: %s (slug: %s)", command.name(), command.slug());

      // Check if product already exists
      var existingProductId = vendureGateway.findProductIdBySlug(command.slug());
      if (existingProductId.isPresent()) {
        LOG.infof("Product with slug '%s' already exists with ID: %s",
            command.slug(), existingProductId.get());
        return new CreateProductResult(false, existingProductId.get(),
            "Product with this slug already exists");
      }

      // Create product in Vendure
      String productId = vendureGateway.createProduct(command);

      if (productId == null) {
        return new CreateProductResult(false, null, "Failed to create product in Vendure");
      }

      // Save locally
      Product product = Product.of(
          productId,
          command.name(),
          command.slug(),
          command.description(),
          true,
          command.featuredAssetId(),
          command.assetIds(),
          List.of());
      productRepository.save(product);

      AssignGroupToProductCommand command2 = new AssignGroupToProductCommand(productId, "1");
      vendureGateway.assignOptionGroupToProduct(command2);

      LOG.infof("Product created successfully with ID: %s", productId);
      return new CreateProductResult(true, productId, null);

    } catch (Exception e) {
      LOG.errorf(e, "Error creating product: %s", command.name());
      return new CreateProductResult(false, null, e.getMessage());
    }
  }

  /**
   * Result of the create product operation.
   */
  public record CreateProductResult(
      boolean success,
      String productId,
      String errorMessage) {
  }
}
