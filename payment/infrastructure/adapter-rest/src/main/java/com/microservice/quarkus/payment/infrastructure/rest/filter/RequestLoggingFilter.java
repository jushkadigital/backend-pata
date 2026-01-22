package com.microservice.quarkus.payment.infrastructure.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JAX-RS filter to log HTTP requests and responses.
 */
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String REQUEST_BODY_PROPERTY = "requestBody";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        String queryParams = requestContext.getUriInfo().getRequestUri().getQuery();

        StringBuilder sb = new StringBuilder();
        sb.append("\n┌─────────────────────────────────────────────────────────────");
        sb.append("\n│ >> REQUEST: ").append(method).append(" /").append(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append("?").append(queryParams);
        }

        // Read and cache request body
        if (hasBody(method)) {
            byte[] bodyBytes = requestContext.getEntityStream().readAllBytes();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);

            // Store for later use and reset stream
            requestContext.setProperty(REQUEST_BODY_PROPERTY, body);
            requestContext.setEntityStream(new ByteArrayInputStream(bodyBytes));

            if (!body.isEmpty()) {
                sb.append("\n│ Body: ").append(formatJson(body));
            }
        }

        System.out.println(sb.toString());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        int status = responseContext.getStatus();
        String statusFamily = getStatusFamily(status);

        StringBuilder sb = new StringBuilder();
        sb.append("\n│ << RESPONSE: ").append(status).append(" ").append(statusFamily);

        // Log response body
        if (responseContext.getEntity() != null) {
            String responseBody = responseContext.getEntity().toString();
            sb.append("\n│ Body: ").append(truncate(responseBody, 500));
        }

        sb.append("\n└─────────────────────────────────────────────────────────────\n");

        if (status >= 400) {
            System.err.println(sb.toString());
        } else {
            System.out.println(sb.toString());
        }
    }

    private boolean hasBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private String getStatusFamily(int status) {
        return switch (status / 100) {
            case 2 -> "OK";
            case 3 -> "REDIRECT";
            case 4 -> "CLIENT ERROR";
            case 5 -> "SERVER ERROR";
            default -> "UNKNOWN";
        };
    }

    private String formatJson(String json) {
        // Simple one-line format, truncate if too long
        String oneLine = json.replaceAll("\\s+", " ").trim();
        return truncate(oneLine, 500);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "null";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "... (truncated)";
    }
}
