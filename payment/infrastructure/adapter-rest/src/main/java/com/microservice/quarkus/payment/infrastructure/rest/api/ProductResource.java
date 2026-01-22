package com.microservice.quarkus.payment.infrastructure.rest.api;

import com.microservice.quarkus.payment.application.port.VendureAdminGateway.CreateProductCommand;
import com.microservice.quarkus.payment.application.port.VendureAdminGateway.CreateVariantCommand;
import com.microservice.quarkus.payment.application.usecase.CreateProductUseCase;
import com.microservice.quarkus.payment.application.usecase.CreateProductVariantsUseCase;
import com.microservice.quarkus.payment.application.usecase.GetProductUseCase;
import com.microservice.quarkus.payment.application.usecase.SearchProductsUseCase;
import com.microservice.quarkus.payment.domain.Money;
import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.infrastructure.rest.dto.CreateProductRequestDTO;
import com.microservice.quarkus.payment.infrastructure.rest.dto.CreateProductResponseDTO;
import com.microservice.quarkus.payment.infrastructure.rest.dto.CreateVariantsRequestDTO;
import com.microservice.quarkus.payment.infrastructure.rest.dto.CreateVariantsResponseDTO;
import com.microservice.quarkus.payment.infrastructure.rest.mapper.PaymentRestMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.Currency;
import java.util.List;

@ApplicationScoped
public class ProductResource implements ProductsAPI {

    @Inject
    GetProductUseCase getProductUseCase;

    @Inject
    SearchProductsUseCase searchProductsUseCase;

    @Inject
    CreateProductUseCase createProductUseCase;

    @Inject
    CreateProductVariantsUseCase createProductVariantsUseCase;

    @Inject
    PaymentRestMapper mapper;

    @Override
    public Response getAllProducts() {
        List<Product> products = getProductUseCase.findAll();
        return Response.ok(mapper.toDTOList(products)).build();
    }

    @Override
    public Response createProduct(CreateProductRequestDTO request) {
        try {
            CreateProductCommand command = new CreateProductCommand(
                    request.getName(),
                    request.getSlug(),
                    request.getDescription(),
                    request.getAssetIds() != null ? request.getAssetIds() : List.of(),
                    request.getFeaturedAssetId()
            );

            var result = createProductUseCase.execute(command);

            CreateProductResponseDTO response = new CreateProductResponseDTO();
            response.setSuccess(result.success());
            response.setProductId(result.productId());
            response.setErrorMessage(result.errorMessage());

            if (result.success()) {
                return Response.status(Response.Status.CREATED).entity(response).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } catch (Exception e) {
            CreateProductResponseDTO response = new CreateProductResponseDTO();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(response).build();
        }
    }

    @Override
    public Response getProductById(String productId) {
        return getProductUseCase.findById(productId)
                .map(product -> Response.ok(mapper.toDTO(product)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Product not found\"}")
                        .build());
    }

    @Override
    public Response getProductBySlug(String slug) {
        return getProductUseCase.findBySlug(slug)
                .map(product -> Response.ok(mapper.toDTO(product)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Product not found\"}")
                        .build());
    }

    @Override
    public Response createProductVariants(String productId, CreateVariantsRequestDTO request) {
        try {
            List<CreateVariantCommand> commands = request.getVariants().stream()
                    .map(v -> {
                        Money price = Money.zero("USD");
                        if (v.getPrice() != null && v.getPrice().getAmount() != null) {
                            price = new Money(
                                    java.math.BigDecimal.valueOf(v.getPrice().getAmount()),
                                    Currency.getInstance(v.getPrice().getCurrency())
                            );
                        }
                        int stockOnHand = v.getStockOnHand() != null ? v.getStockOnHand() : 100;
                        return new CreateVariantCommand(v.getSku(), v.getName(), price, stockOnHand);
                    })
                    .toList();

            var result = createProductVariantsUseCase.execute(productId, commands);

            CreateVariantsResponseDTO response = new CreateVariantsResponseDTO();
            response.setSuccess(result.success());
            response.setVariantIds(result.variantIds());
            response.setErrorMessage(result.errorMessage());

            if (result.success()) {
                return Response.status(Response.Status.CREATED).entity(response).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } catch (Exception e) {
            CreateVariantsResponseDTO response = new CreateVariantsResponseDTO();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(response).build();
        }
    }

    @Override
    public Response searchProducts(String q) {
        List<Product> products = searchProductsUseCase.search(q);
        return Response.ok(mapper.toDTOList(products)).build();
    }
}
