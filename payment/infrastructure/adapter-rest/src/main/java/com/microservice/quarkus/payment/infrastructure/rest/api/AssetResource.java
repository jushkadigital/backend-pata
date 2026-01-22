package com.microservice.quarkus.payment.infrastructure.rest.api;

import com.microservice.quarkus.payment.application.usecase.UploadAssetUseCase;
import com.microservice.quarkus.payment.infrastructure.rest.dto.AssetResponseDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

@ApplicationScoped
public class AssetResource implements AssetsAPI {

    @Inject
    UploadAssetUseCase uploadAssetUseCase;

    @Override
    public Response uploadAsset(InputStream fileInputStream, String fileName) {
        try {
            if (fileInputStream == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"No file provided\"}")
                        .build();
            }

            if (fileName == null || fileName.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"fileName is required\"}")
                        .build();
            }

            var result = uploadAssetUseCase.execute(fileInputStream, fileName);

            if (result.success()) {
                AssetResponseDTO response = new AssetResponseDTO();
                response.setId(result.asset().id());
                response.setName(result.asset().name());
                response.setSource(result.asset().source());
                response.setPreview(result.asset().preview());
                return Response.status(Response.Status.CREATED).entity(response).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"" + result.errorMessage() + "\"}")
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\": \"Failed to upload asset: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
