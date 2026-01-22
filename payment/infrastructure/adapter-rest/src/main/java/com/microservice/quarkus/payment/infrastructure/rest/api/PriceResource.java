package com.microservice.quarkus.payment.infrastructure.rest.api;

import com.microservice.quarkus.payment.application.dto.PriceQuery;
import com.microservice.quarkus.payment.application.usecase.GetProductPriceUseCase;
import com.microservice.quarkus.payment.application.usecase.GetProductUseCase;
import com.microservice.quarkus.payment.domain.Money;
import com.microservice.quarkus.payment.infrastructure.rest.dto.CalculatePriceRequestDTO;
import com.microservice.quarkus.payment.infrastructure.rest.dto.PriceItemDTO;
import com.microservice.quarkus.payment.infrastructure.rest.dto.PriceResponseDTO;
import com.microservice.quarkus.payment.infrastructure.rest.mapper.PaymentRestMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.Optional;

@ApplicationScoped
public class PriceResource implements PricesAPI {

    @Inject
    GetProductPriceUseCase getProductPriceUseCase;

    @Inject
    GetProductUseCase getProductUseCase;

    @Inject
    PaymentRestMapper mapper;

    @Override
    public Response getProductPrice(String productId) {
        Optional<Money> priceOpt = getProductPriceUseCase.getDefaultPrice(productId);

        if (priceOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Product not found or has no price\"}")
                    .build();
        }

        Money price = priceOpt.get();
        PriceResponseDTO response = new PriceResponseDTO();
        response.setProductId(productId);
        response.setPrice(mapper.toMoneyDTO(price));

        return Response.ok(response).build();
    }

    @Override
    public Response getVariantPrice(String productId, String variantId) {
        Optional<Money> priceOpt = getProductPriceUseCase.execute(new PriceQuery(productId, variantId, 1));

        if (priceOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Product or variant not found\"}")
                    .build();
        }

        Money price = priceOpt.get();
        PriceResponseDTO response = new PriceResponseDTO();
        response.setProductId(productId);
        response.setVariantId(variantId);
        response.setPrice(mapper.toMoneyDTO(price));

        return Response.ok(response).build();
    }

    @Override
    public Response calculateTotal(CalculatePriceRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Items list cannot be empty\"}")
                    .build();
        }

        Money total = null;

        for (PriceItemDTO item : request.getItems()) {
            Optional<Money> itemPrice;

            if (item.getSku() != null && !item.getSku().isBlank()) {
                // Search by SKU
                itemPrice = getProductUseCase.findBySku(item.getSku())
                        .map(variant -> variant.price().multiply(item.getQuantity()));
            } else if (item.getProductId() != null) {
                if (item.getVariantId() != null) {
                    itemPrice = getProductPriceUseCase.calculateVariantTotal(
                            item.getProductId(), item.getVariantId(), item.getQuantity());
                } else {
                    itemPrice = getProductPriceUseCase.calculateTotal(
                            item.getProductId(), item.getQuantity());
                }
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Each item must have productId or sku\"}")
                        .build();
            }

            if (itemPrice.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Product not found: " +
                                (item.getSku() != null ? item.getSku() : item.getProductId()) + "\"}")
                        .build();
            }

            if (total == null) {
                total = itemPrice.get();
            } else {
                total = total.add(itemPrice.get());
            }
        }

        PriceResponseDTO response = new PriceResponseDTO();
        response.setTotal(mapper.toMoneyDTO(total));
        response.setQuantity(request.getItems().stream()
                .mapToInt(PriceItemDTO::getQuantity)
                .sum());

        return Response.ok(response).build();
    }

    @Override
    public Response getPriceBySku(String sku) {
        return getProductUseCase.findBySku(sku)
                .map(variant -> {
                    PriceResponseDTO response = new PriceResponseDTO();
                    response.setSku(sku);
                    response.setPrice(mapper.toMoneyDTO(variant.price()));
                    return Response.ok(response).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"SKU not found: " + sku + "\"}")
                        .build());
    }
}
