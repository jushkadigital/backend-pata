package com.microservice.quarkus.user.iam.infrastructure.webhook.event;

import java.util.Map;
import java.util.UUID;

public record WebhookPayloadPayload(
    String email,
    String password,
    String type,
    String clientId) {
}
