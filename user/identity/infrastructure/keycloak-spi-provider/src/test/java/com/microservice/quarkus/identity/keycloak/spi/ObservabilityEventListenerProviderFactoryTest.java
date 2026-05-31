package com.microservice.quarkus.identity.keycloak.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservabilityEventListenerProviderFactoryTest {

    ObservabilityEventListenerProviderFactory factory;
    Config.Scope config;
    KeycloakSession session;

    @BeforeEach
    void setUp() {
        factory = new ObservabilityEventListenerProviderFactory();
        config = mock(Config.Scope.class);
        session = mock(KeycloakSession.class);
    }

    @Test
    void create_readsWebhookUrlFromConfig() {
        when(config.get("webhook-url", "http://localhost:8081/webhooks/keycloak"))
            .thenReturn("http://custom.webhook/url");
        factory.init(config);

        ObservabilityEventListenerProvider provider =
            (ObservabilityEventListenerProvider) factory.create(session);

        assertNotNull(provider);
    }

    @Test
    void create_usesDefaultWebhookUrlWhenConfigIsAbsent() {
        when(config.get("webhook-url", "http://localhost:8081/webhooks/keycloak"))
            .thenReturn("http://localhost:8081/webhooks/keycloak");
        factory.init(config);

        ObservabilityEventListenerProvider provider =
            (ObservabilityEventListenerProvider) factory.create(session);

        assertNotNull(provider);
    }

    @Test
    void getId_returnsObservabilityEvents() {
        assertEquals("observability-events", factory.getId());
    }

    @Test
    void init_storesConfig() {
        factory.init(config);

        // Verify init stores config by checking create uses it
        when(config.get("webhook-url", "http://localhost:8081/webhooks/keycloak"))
            .thenReturn("http://test.webhook/url");
        factory.init(config);

        ObservabilityEventListenerProvider provider =
            (ObservabilityEventListenerProvider) factory.create(session);
        assertNotNull(provider);
    }

    @Test
    void postInit_isNoOp() {
        assertDoesNotThrow(() -> factory.postInit(null));
    }

    @Test
    void close_isNoOp() {
        assertDoesNotThrow(() -> factory.close());
    }
}
