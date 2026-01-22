package com.microservice.quarkus.payment.application.usecase;

import com.microservice.quarkus.payment.application.port.VendureAdminGateway;
import com.microservice.quarkus.payment.application.port.VendureAdminGateway.AssetResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Use case for uploading assets (images) to Vendure.
 */
@ApplicationScoped
public class UploadAssetUseCase {

    private static final Logger LOG = Logger.getLogger(UploadAssetUseCase.class);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);

    @Inject
    VendureAdminGateway vendureGateway;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Downloads an image from URL and uploads it to Vendure.
     *
     * @param imageUrl URL of the image to download
     * @param customFileName Optional custom filename (can be null)
     * @return Result of the upload
     */
    public UploadResult executeFromUrl(String imageUrl, String customFileName) {
        try {
            LOG.infof("Downloading image from URL: %s", imageUrl);

            // Download the image
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                return new UploadResult(false, null,
                        "Failed to download image. HTTP status: " + response.statusCode());
            }

            // Determine filename
            String fileName = customFileName;
            if (fileName == null || fileName.isBlank()) {
                fileName = extractFileNameFromUrl(imageUrl);
            }

            LOG.infof("Uploading downloaded image as: %s", fileName);

            // Upload to Vendure
            try (InputStream imageStream = response.body()) {
                AssetResult result = vendureGateway.createAsset(imageStream, fileName);

                if (result == null) {
                    return new UploadResult(false, null, "Failed to upload asset to Vendure");
                }

                LOG.infof("Asset uploaded successfully with ID: %s", result.id());
                return new UploadResult(true, result, null);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error downloading/uploading asset from URL: %s", imageUrl);
            return new UploadResult(false, null, e.getMessage());
        }
    }

    /**
     * Extracts filename from URL.
     */
    private String extractFileNameFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                String fileName = path.substring(lastSlash + 1);
                // Remove query params if any
                int queryIndex = fileName.indexOf('?');
                if (queryIndex > 0) {
                    fileName = fileName.substring(0, queryIndex);
                }
                if (!fileName.isBlank()) {
                    return fileName;
                }
            }
        } catch (Exception e) {
            LOG.warnf("Could not extract filename from URL: %s", url);
        }
        // Default filename if extraction fails
        return "image_" + System.currentTimeMillis() + ".jpg";
    }

    /**
     * Uploads a single asset to Vendure from InputStream.
     *
     * @param file The file input stream
     * @param fileName The file name
     * @return Result of the upload
     */
    public UploadResult execute(InputStream file, String fileName) {
        try {
            LOG.infof("Uploading asset: %s", fileName);

            AssetResult result = vendureGateway.createAsset(file, fileName);

            if (result == null) {
                return new UploadResult(false, null, "Failed to upload asset to Vendure");
            }

            LOG.infof("Asset uploaded successfully with ID: %s", result.id());
            return new UploadResult(true, result, null);

        } catch (Exception e) {
            LOG.errorf(e, "Error uploading asset: %s", fileName);
            return new UploadResult(false, null, e.getMessage());
        }
    }

    /**
     * Uploads multiple assets to Vendure.
     *
     * @param files List of file input streams
     * @param fileNames List of file names
     * @return Result of the upload
     */
    public UploadMultipleResult executeMultiple(List<InputStream> files, List<String> fileNames) {
        try {
            LOG.infof("Uploading %d assets", files.size());

            if (files.size() != fileNames.size()) {
                return new UploadMultipleResult(false, List.of(),
                    "Number of files and file names must match");
            }

            List<AssetResult> results = vendureGateway.createAssets(files, fileNames);

            if (results == null || results.isEmpty()) {
                return new UploadMultipleResult(false, List.of(),
                    "Failed to upload assets to Vendure");
            }

            LOG.infof("Uploaded %d assets successfully", results.size());
            return new UploadMultipleResult(true, results, null);

        } catch (Exception e) {
            LOG.errorf(e, "Error uploading assets");
            return new UploadMultipleResult(false, List.of(), e.getMessage());
        }
    }

    /**
     * Result of a single asset upload.
     */
    public record UploadResult(
        boolean success,
        AssetResult asset,
        String errorMessage
    ) {}

    /**
     * Result of multiple asset uploads.
     */
    public record UploadMultipleResult(
        boolean success,
        List<AssetResult> assets,
        String errorMessage
    ) {}
}
