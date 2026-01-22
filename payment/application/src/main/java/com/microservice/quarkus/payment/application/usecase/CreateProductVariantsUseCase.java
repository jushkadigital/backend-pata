package com.microservice.quarkus.payment.application.usecase;

import com.microservice.quarkus.payment.application.port.VendureAdminGateway;
import com.microservice.quarkus.payment.application.port.VendureAdminGateway.CreateVariantCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Use case for creating product variants in Vendure.
 */
@ApplicationScoped
public class CreateProductVariantsUseCase {

    private static final Logger LOG = Logger.getLogger(CreateProductVariantsUseCase.class);

    @Inject
    VendureAdminGateway vendureGateway;

    /**
     * Creates variants for a product in Vendure.
     *
     * @param productId The product ID
     * @param variants List of variants to create
     * @return Result of the creation
     */
    public CreateVariantsResult execute(String productId, List<CreateVariantCommand> variants) {
        try {
            LOG.infof("Creating %d variants for product: %s", variants.size(), productId);

            if (variants.isEmpty()) {
                return new CreateVariantsResult(false, List.of(), "No variants provided");
            }

            List<String> variantIds = vendureGateway.createProductVariants(productId, variants);

            if (variantIds == null || variantIds.isEmpty()) {
                return new CreateVariantsResult(false, List.of(), "Failed to create variants in Vendure");
            }

            LOG.infof("Created %d variants for product %s: %s", variantIds.size(), productId, variantIds);
            return new CreateVariantsResult(true, variantIds, null);

        } catch (Exception e) {
            LOG.errorf(e, "Error creating variants for product: %s", productId);
            return new CreateVariantsResult(false, List.of(), e.getMessage());
        }
    }

    /**
     * Result of the create variants operation.
     */
    public record CreateVariantsResult(
        boolean success,
        List<String> variantIds,
        String errorMessage
    ) {}
}
