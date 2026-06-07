package com.microservice.quarkus.identity.keycloak.spi;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ObservabilityEventListenerProviderTest {

    @RegisterExtension
    static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    KeycloakSession session;
    KeycloakTransactionManager transactionManager;
    ObservabilityEventListenerProvider provider;

    @BeforeEach
    void setUp() {
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(otelTesting.getOpenTelemetry());

        session = mock(KeycloakSession.class);
        transactionManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(transactionManager);

        provider = new ObservabilityEventListenerProvider(session, "http://localhost:9999/webhook");
    }

    private Event createEvent(EventType type) {
        Event event = new Event();
        event.setType(type);
        event.setRealmId("test-realm");
        event.setClientId("test-client");
        event.setUserId("user-123");
        event.setSessionId("session-456");
        event.setIpAddress("127.0.0.1");
        event.setTime(System.currentTimeMillis());
        event.setId("event-789");
        return event;
    }

    private AdminEvent createAdminEvent() {
        return createAdminEvent(OperationType.CREATE, ResourceType.USER, "users/new-user-123");
    }

    private AdminEvent createAdminEvent(OperationType operationType, ResourceType resourceType, String resourcePath) {
        AdminEvent event = new AdminEvent();
        event.setOperationType(operationType);
        event.setResourceType(resourceType);
        event.setResourcePath(resourcePath);
        event.setRealmId("test-realm");
        event.setTime(System.currentTimeMillis());
        event.setId("admin-event-123");
        AuthDetails authDetails = new AuthDetails();
        authDetails.setClientId("auth-client-uuid");
        authDetails.setUserId("admin-user-456");
        authDetails.setIpAddress("10.0.0.1");
        event.setAuthDetails(authDetails);
        return event;
    }

    @Test
    void onEvent_register_createsSpanWithCorrectAttributes_andIncrementsCounter_andEnlistsWebhook() {
        Event event = createEvent(EventType.REGISTER);

        provider.onEvent(event);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("keycloak.event.register", span.getName());
        assertEquals("REGISTER", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.event.type")));
        assertEquals("test-realm", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.realm.id")));
        assertEquals("test-client", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.client.id")));
        assertEquals("user-123", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.user.id")));
        assertEquals("session-456", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.session.id")));
        assertEquals("127.0.0.1", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.ip.address")));

        Counter counter = provider.getMeterRegistry().find("keycloak.identity.user.created.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        verify(transactionManager).enlistAfterCompletion(any(AbstractKeycloakTransaction.class));
    }

    @Test
    void onEvent_adminDeleteUser_createsSpan_andIncrementsCounter_andEnlistsWebhook() {
        AdminEvent event = createAdminEvent(OperationType.DELETE, ResourceType.USER, "users/deleted-user-789");

        provider.onEvent(event, false);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertEquals("keycloak.admin_event.delete", spans.get(0).getName());

        Counter counter = provider.getMeterRegistry().find("keycloak.identity.user.deleted.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        verify(transactionManager).enlistAfterCompletion(any(AbstractKeycloakTransaction.class));
    }

    @Test
    void onEvent_adminDeleteNonUser_doesNotEnlistWebhook() {
        AdminEvent event = createAdminEvent(OperationType.DELETE, ResourceType.CLIENT, "clients/some-client");

        provider.onEvent(event, false);

        Counter counter = provider.getMeterRegistry().find("keycloak.identity.user.deleted.total").counter();
        assertNotNull(counter);
        assertEquals(0.0, counter.count());

        verify(transactionManager, never()).enlistAfterCompletion(any(AbstractKeycloakTransaction.class));
    }

    @Test
    void onEvent_login_createsSpan_andIncrementsCounter_andDoesNotEnlistWebhook() {
        Event event = createEvent(EventType.LOGIN);

        provider.onEvent(event);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertEquals("keycloak.event.login", spans.get(0).getName());

        Counter counter = provider.getMeterRegistry().find("keycloak.identity.user.login.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        verify(transactionManager, never()).enlistAfterCompletion(any(AbstractKeycloakTransaction.class));
    }

    @Test
    void onEvent_loginError_createsSpanWithErrorStatus_andIncrementsCounter() {
        Event event = createEvent(EventType.LOGIN_ERROR);
        event.setError("invalid_credentials");

        provider.onEvent(event);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("keycloak.event.login_error", span.getName());
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("invalid_credentials", span.getStatus().getDescription());

        Counter counter = provider.getMeterRegistry().find("keycloak.identity.user.login.error.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void onEvent_withErrorDetails_setsErrorAttributeAndStatus() {
        Event event = createEvent(EventType.LOGIN);
        event.setError("some_error");

        provider.onEvent(event);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("some_error", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.event.error")));
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
    }

    @Test
    void onEvent_withDetailsMap_setsDetailAttributes() {
        Event event = createEvent(EventType.LOGIN);
        Map<String, String> details = new HashMap<>();
        details.put("username", "testuser");
        details.put("auth_method", "password");
        event.setDetails(details);

        provider.onEvent(event);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("testuser", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.details.username")));
        assertEquals("password", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.details.auth_method")));
    }

    @Test
    void onEvent_adminEvent_createsSpanWithCorrectAttributes() {
        AdminEvent event = createAdminEvent();

        provider.onEvent(event, false);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("keycloak.admin_event.create", span.getName());
        assertEquals("USER", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.admin.resource_type")));
        assertEquals("CREATE", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.admin.operation")));
        assertEquals("test-realm", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.realm.id")));
    }

    @Test
    void onEvent_adminEvent_withError_setsErrorStatus() {
        AdminEvent event = createAdminEvent();
        event.setError("admin_error");

        provider.onEvent(event, false);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("admin_error", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("keycloak.admin.error")));
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
    }

    @Test
    void close_isNoOp() {
        assertDoesNotThrow(() -> provider.close());
    }

    @Test
    void webhook_sendsW3CTraceContextHeaders() throws Exception {
        AtomicReference<String> traceparentHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/webhook", exchange -> {
            traceparentHeader.set(exchange.getRequestHeaders().getFirst("traceparent"));
            try (InputStream is = exchange.getRequestBody()) {
                requestBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();

        try {
            String webhookUrl = "http://localhost:" + server.getAddress().getPort() + "/webhook";
            ObservabilityEventListenerProvider webhookProvider = new ObservabilityEventListenerProvider(session, webhookUrl);

            Event event = createEvent(EventType.REGISTER);

            // Capture the transaction and manually commit it
            org.mockito.ArgumentCaptor<AbstractKeycloakTransaction> txCaptor = org.mockito.ArgumentCaptor.forClass(AbstractKeycloakTransaction.class);
            webhookProvider.onEvent(event);
            verify(transactionManager).enlistAfterCompletion(txCaptor.capture());
            txCaptor.getValue().begin();
            txCaptor.getValue().commit();

            // Give the async HTTP call a moment to complete
            Thread.sleep(200);

            assertNotNull(traceparentHeader.get(), "traceparent header should be present");
            assertTrue(traceparentHeader.get().startsWith("00-"), "traceparent should start with W3C version 00");

            // Verify payload structure
            String body = requestBody.get();
            assertNotNull(body);
            assertTrue(body.contains("\"type\":\"REGISTER\""));
            assertTrue(body.contains("\"realmId\":\"test-realm\""));
            assertTrue(body.contains("\"clientId\":\"test-client\""));
            assertTrue(body.contains("\"userId\":\"user-123\""));
            assertTrue(body.contains("\"ipAddress\":\"127.0.0.1\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void webhook_failureIsHandledGracefully() {
        // Use a URL that will cause connection failure
        ObservabilityEventListenerProvider failingProvider = new ObservabilityEventListenerProvider(session, "http://localhost:1/webhook");

        Event event = createEvent(EventType.REGISTER);

        org.mockito.ArgumentCaptor<AbstractKeycloakTransaction> txCaptor = org.mockito.ArgumentCaptor.forClass(AbstractKeycloakTransaction.class);
        failingProvider.onEvent(event);
        verify(transactionManager).enlistAfterCompletion(txCaptor.capture());

        // Should not throw even though the webhook URL is invalid
        assertDoesNotThrow(() -> {
            AbstractKeycloakTransaction tx = txCaptor.getValue();
            tx.begin();
            tx.commit();
        });
    }

    @Test
    void webhook_adminDeleteUser_payloadMatchesExpectedJsonStructure() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/webhook", exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                requestBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();

        try {
            String webhookUrl = "http://localhost:" + server.getAddress().getPort() + "/webhook";
            ObservabilityEventListenerProvider webhookProvider = new ObservabilityEventListenerProvider(session, webhookUrl);

            AdminEvent event = createAdminEvent(OperationType.DELETE, ResourceType.USER, "users/deleted-user-789");

            org.mockito.ArgumentCaptor<AbstractKeycloakTransaction> txCaptor = org.mockito.ArgumentCaptor.forClass(AbstractKeycloakTransaction.class);
            webhookProvider.onEvent(event, false);
            verify(transactionManager).enlistAfterCompletion(txCaptor.capture());
            txCaptor.getValue().begin();
            txCaptor.getValue().commit();

            Thread.sleep(200);

            String body = requestBody.get();
            assertNotNull(body);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> payload = mapper.readValue(body, Map.class);
            assertEquals("DELETE", payload.get("type"));
            assertEquals("deleted-user-789", payload.get("userId"));
            assertEquals("test-realm", payload.get("realmId"));
            assertEquals("auth-client-uuid", payload.get("clientId"));
            assertEquals("10.0.0.1", payload.get("ipAddress"));
            assertNotNull(payload.get("time"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void webhook_retriesThenSucceedsOnThirdAttempt() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockClient.send(any(HttpRequest.class), any()))
            .thenThrow(new IOException("Connection refused"))
            .thenThrow(new IOException("Connection refused"))
            .thenAnswer(invocation -> mockResponse);

        ObservabilityEventListenerProvider retryProvider = new ObservabilityEventListenerProvider(
            session, "http://localhost:9999/webhook", new SimpleMeterRegistry(), mockClient);

        Event event = createEvent(EventType.REGISTER);

        org.mockito.ArgumentCaptor<AbstractKeycloakTransaction> txCaptor = org.mockito.ArgumentCaptor.forClass(AbstractKeycloakTransaction.class);
        retryProvider.onEvent(event);
        verify(transactionManager).enlistAfterCompletion(txCaptor.capture());

        txCaptor.getValue().begin();
        txCaptor.getValue().commit();

        verify(mockClient, times(3)).send(any(HttpRequest.class), any());
    }

    @Test
    void webhook_allRetriesFail() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);

        when(mockClient.send(any(HttpRequest.class), any()))
            .thenThrow(new IOException("Connection refused"));

        ObservabilityEventListenerProvider retryProvider = new ObservabilityEventListenerProvider(
            session, "http://localhost:9999/webhook", new SimpleMeterRegistry(), mockClient);

        Event event = createEvent(EventType.REGISTER);

        org.mockito.ArgumentCaptor<AbstractKeycloakTransaction> txCaptor = org.mockito.ArgumentCaptor.forClass(AbstractKeycloakTransaction.class);
        retryProvider.onEvent(event);
        verify(transactionManager).enlistAfterCompletion(txCaptor.capture());

        assertDoesNotThrow(() -> {
            txCaptor.getValue().begin();
            txCaptor.getValue().commit();
        });

        verify(mockClient, times(3)).send(any(HttpRequest.class), any());
    }
}
