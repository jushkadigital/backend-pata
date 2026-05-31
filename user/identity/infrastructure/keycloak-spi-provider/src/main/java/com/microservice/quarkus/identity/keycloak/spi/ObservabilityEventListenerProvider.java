package com.microservice.quarkus.identity.keycloak.spi;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

public class ObservabilityEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(ObservabilityEventListenerProvider.class);
    private static final String TRACER_NAME = "keycloak-identity-spi";
    
    private final KeycloakSession session;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;
    private final String webhookUrl;
    private final HttpClient httpClient;
    
    // Counters
    private final Counter registerCounter;
    private final Counter deleteCounter;
    private final Counter loginCounter;
    private final Counter loginErrorCounter;
    
    public ObservabilityEventListenerProvider(KeycloakSession session, String webhookUrl) {
        this(session, webhookUrl, new SimpleMeterRegistry());
    }

    ObservabilityEventListenerProvider(KeycloakSession session, String webhookUrl, MeterRegistry meterRegistry) {
        this(session, webhookUrl, meterRegistry, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build());
    }

    ObservabilityEventListenerProvider(KeycloakSession session, String webhookUrl, MeterRegistry meterRegistry, HttpClient httpClient) {
        this.session = session;
        this.webhookUrl = webhookUrl;
        this.tracer = GlobalOpenTelemetry.getTracer(TRACER_NAME);
        this.meterRegistry = meterRegistry;
        this.httpClient = httpClient;

        this.registerCounter = Counter.builder("keycloak.identity.user.registered.total")
            .description("Total user registrations via Keycloak")
            .tag("realm", "quarkus")
            .register(meterRegistry);
        this.deleteCounter = Counter.builder("keycloak.identity.user.deleted.total")
            .description("Total user deletions via Keycloak")
            .tag("realm", "quarkus")
            .register(meterRegistry);
        this.loginCounter = Counter.builder("keycloak.identity.user.login.total")
            .description("Total user logins via Keycloak")
            .tag("realm", "quarkus")
            .register(meterRegistry);
        this.loginErrorCounter = Counter.builder("keycloak.identity.user.login.error.total")
            .description("Total failed user login attempts via Keycloak")
            .tag("realm", "quarkus")
            .register(meterRegistry);
    }
    
    MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    @Override
    public void onEvent(Event event) {
        String spanName = "keycloak.event." + event.getType().name().toLowerCase();
        
        SpanBuilder spanBuilder = tracer.spanBuilder(spanName)
            .setAttribute("keycloak.event.type", event.getType().name())
            .setAttribute("keycloak.realm.id", event.getRealmId() != null ? event.getRealmId() : "")
            .setAttribute("keycloak.client.id", event.getClientId() != null ? event.getClientId() : "");
        
        if (event.getUserId() != null) {
            spanBuilder.setAttribute("keycloak.user.id", event.getUserId());
        }
        if (event.getSessionId() != null) {
            spanBuilder.setAttribute("keycloak.session.id", event.getSessionId());
        }
        if (event.getIpAddress() != null) {
            spanBuilder.setAttribute("keycloak.ip.address", event.getIpAddress());
        }
        
        Span span = spanBuilder.startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Metrics
            if (event.getType() == EventType.REGISTER) {
                registerCounter.increment();
                sendWebhookAfterCommit(event);
            } else if (event.getType() == EventType.DELETE_ACCOUNT) {
                deleteCounter.increment();
                sendWebhookAfterCommit(event);
            } else if (event.getType() == EventType.LOGIN) {
                loginCounter.increment();
            } else if (event.getType() == EventType.LOGIN_ERROR) {
                loginErrorCounter.increment();
                span.setStatus(StatusCode.ERROR, event.getError());
            }
            
            // Add error details for error events
            if (event.getError() != null) {
                span.setAttribute("keycloak.event.error", event.getError());
                span.setStatus(StatusCode.ERROR, event.getError());
            }
            
            // Add event details as span attributes
            if (event.getDetails() != null) {
                event.getDetails().forEach((k, v) -> 
                    span.setAttribute("keycloak.details." + k, v != null ? v : ""));
            }
        } finally {
            span.end();
        }
    }
    
    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        Span span = tracer.spanBuilder("keycloak.admin_event." + event.getOperationType().name().toLowerCase())
            .setAttribute("keycloak.admin.resource_type", event.getResourceType().name())
            .setAttribute("keycloak.admin.operation", event.getOperationType().name())
            .setAttribute("keycloak.realm.id", event.getRealmId() != null ? event.getRealmId() : "")
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            if (event.getError() != null) {
                span.setAttribute("keycloak.admin.error", event.getError());
                span.setStatus(StatusCode.ERROR, event.getError());
            }
        } finally {
            span.end();
        }
    }
    
    @Override
    public void close() {
        // No-op
    }
    
    private void sendWebhookAfterCommit(Event event) {
        Context currentContext = Context.current();
        session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
            @Override
            protected void commitImpl() {
                sendWebhookWithTraceContext(event, currentContext);
            }
            
            @Override
            protected void rollbackImpl() {
                // No-op on rollback
            }
        });
    }
    
    private void sendWebhookWithTraceContext(Event event, Context context) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String payload = buildEventPayload(event);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

                TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
                Map<String, String> headers = new HashMap<>();
                propagator.inject(context, headers, (carrier, key, value) -> carrier.put(key, value));
                headers.forEach(requestBuilder::header);

                HttpRequest request = requestBuilder.build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                log.infof("Webhook sent for event %s with trace context (attempt %d/%d)",
                    event.getType(), attempt, maxRetries);
                return; // Success — exit retry loop
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    long delayMs = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s exponential backoff
                    log.warnf("Webhook attempt %d/%d failed for event %s: %s — retrying in %dms",
                        attempt, maxRetries, event.getType(), e.getMessage(), delayMs);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warnf("Webhook retry interrupted for event %s", event.getType());
                        return;
                    }
                } else {
                    log.errorf("Webhook failed after %d attempts for event %s: %s",
                        maxRetries, event.getType(), e.getMessage());
                }
            }
        }
    }
    
    private String buildEventPayload(Event event) {
        // Build JSON matching the existing KeycloakDTO structure the webhook controller expects
        Map<String, Object> payload = new HashMap<>();
        if (event.getId() != null) payload.put("id", event.getId());
        payload.put("time", event.getTime());
        payload.put("type", event.getType().name());
        if (event.getRealmId() != null) payload.put("realmId", event.getRealmId());
        if (event.getClientId() != null) payload.put("clientId", event.getClientId());
        if (event.getUserId() != null) payload.put("userId", event.getUserId());
        if (event.getIpAddress() != null) payload.put("ipAddress", event.getIpAddress());
        if (event.getDetails() != null) payload.put("details", event.getDetails());
        if (event.getError() != null) payload.put("error", event.getError());
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            log.warnf("Failed to serialize event payload: %s", e.getMessage());
            return "{}";
        }
    }
}
