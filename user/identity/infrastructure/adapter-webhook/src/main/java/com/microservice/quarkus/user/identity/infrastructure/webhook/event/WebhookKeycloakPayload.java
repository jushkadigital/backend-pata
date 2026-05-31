package com.microservice.quarkus.user.identity.infrastructure.webhook.event;

import java.util.Map;
import java.util.UUID;

public record WebhookKeycloakPayload(
    String eventType, // REGISTER, UPDATE, etc.
    String userId,
    String clientId,
    Map<String, String> data,
    String traceParent,   // W3C traceparent header
    String traceState) {  // W3C tracestate header
}
