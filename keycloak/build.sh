#!/bin/bash
set -e

# Build the SPI provider JAR
echo "Building keycloak-spi-provider..."
mvn clean package -pl user/identity/infrastructure/keycloak-spi-provider -DskipTests

# Copy the JAR to the persistent providers directory (used by compose-devservices.yml)
mkdir -p keycloak/providers
cp user/identity/infrastructure/keycloak-spi-provider/target/keycloak-spi-provider-0.0.1-SNAPSHOT.jar keycloak/providers/keycloak-spi-provider.jar

echo "SPI JAR copied to keycloak/providers/"

# Build the Docker image (for prod)
echo "Building custom Keycloak Docker image..."
docker build -t keycloak-with-spi:26.4.2 keycloak/

echo "Done! Image: keycloak-with-spi:26.4.2"
