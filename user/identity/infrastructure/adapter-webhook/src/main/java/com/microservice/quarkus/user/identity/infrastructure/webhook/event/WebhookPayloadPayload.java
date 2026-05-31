package com.microservice.quarkus.user.identity.infrastructure.webhook.event;

import java.util.Map;
import java.util.UUID;

public record WebhookPayloadPayload(
    String email,
    String password,
    String type,
    String clientId) {
}
