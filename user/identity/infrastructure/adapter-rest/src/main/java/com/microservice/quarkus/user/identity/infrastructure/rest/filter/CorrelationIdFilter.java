package com.microservice.quarkus.user.identity.infrastructure.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import java.util.UUID;

@Provider
public class CorrelationIdFilter implements ContainerRequestFilter {

  private static final String HEADER = "x-correlation-id";

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String incoming = requestContext.getHeaderString(HEADER);
    String correlationId = (incoming != null && !incoming.isBlank())
        ? incoming
        : UUID.randomUUID().toString();

    MDC.put("correlationId", correlationId);
  }
}
