package com.microservice.quarkus.user.iam.infrastructure.webhook.event;

import java.util.Map;
import java.util.UUID;

public record WebhookKeycloakPayload(
    String eventType, // REGISTER, UPDATE, etc.
    String userId,
    String clientId,
    Map<String, String> data) {
}
