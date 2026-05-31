#!/bin/bash
set -e

# Ensure SPI provider JAR exists in the persistent directory before starting dev services
SPI_SOURCE="user/identity/infrastructure/keycloak-spi-provider/target/keycloak-spi-provider-0.0.1-SNAPSHOT.jar"
SPI_DEST="keycloak/providers/keycloak-spi-provider.jar"

if [ ! -f "$SPI_DEST" ]; then
  echo "Building Keycloak SPI provider and copying to keycloak/providers/..."
  mvn package -pl user/identity/infrastructure/keycloak-spi-provider -DskipTests -q
  mkdir -p keycloak/providers
  cp "$SPI_SOURCE" "$SPI_DEST"
fi

# Start observability stack (optional)
if [ "$1" == "--with-observability" ]; then
  echo "Starting observability stack..."
  docker compose -f docker-compose.observability.yml up -d
  echo "Waiting for observability services..."
  sleep 5
fi

echo "Starting Quarkus dev mode..."
mvn quarkus:dev -pl bootloader -Dquarkus.http.port=8081 -Dquarkus.enableGlobal=false
