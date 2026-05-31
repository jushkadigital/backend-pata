FROM quay.io/keycloak/keycloak:26.4.2 AS builder

# Copy custom SPI provider
COPY keycloak-spi-provider.jar /opt/keycloak/providers/

# Rebuild Keycloak with the custom provider
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.4.2
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD curl -f http://localhost:8080/health/ready || exit 1
