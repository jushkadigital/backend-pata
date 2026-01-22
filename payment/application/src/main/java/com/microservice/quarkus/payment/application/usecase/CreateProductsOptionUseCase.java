package com.microservice.quarkus.payment.application.usecase;

import com.microservice.quarkus.payment.application.port.VendureAdminGateway;
import com.microservice.quarkus.payment.application.port.VendureAdminGateway.CreateProductCommand;
import com.microservice.quarkus.payment.application.port.VendureAdminGateway.CreateProductsOptionCommand;
import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Use case for creating a product in Vendure and storing it locally.
 */
@ApplicationScoped
public class CreateProductsOptionUseCase {

  private static final Logger LOG = Logger.getLogger(CreateProductsOptionUseCase.class);

  @Inject
  VendureAdminGateway vendureGateway;

  /**
   * Creates a product in Vendure and saves it locally.
   *
   * @param command The create product command
   * @return Result of the creation
   */
  public CreateProductResult execute(CreateProductsOptionCommand command) {
    try {
      LOG.infof("Creating product option: %s ", command.name());

      // Create product in Vendure
      String productId = vendureGateway.createProductsOption(command);

      if (productId == null) {
        return new CreateProductResult(false, null, "Failed to create product in Vendure");
      }
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
