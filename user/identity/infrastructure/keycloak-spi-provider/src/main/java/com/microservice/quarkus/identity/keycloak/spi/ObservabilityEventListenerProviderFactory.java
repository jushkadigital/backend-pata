package com.microservice.quarkus.identity.keycloak.spi;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ObservabilityEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "observability-events";
    
    private Config.Scope config;
    
    @Override
    public EventListenerProvider create(KeycloakSession session) {
        String webhookUrl = config.get("webhook-url", "http://localhost:8081/webhooks/keycloak");
        return new ObservabilityEventListenerProvider(session, webhookUrl);
    }
    
    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No-op
    }
    
    @Override
    public void close() {
        // No-op
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
