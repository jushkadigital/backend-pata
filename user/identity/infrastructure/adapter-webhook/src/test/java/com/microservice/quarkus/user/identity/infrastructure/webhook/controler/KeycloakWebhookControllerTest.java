package com.microservice.quarkus.user.identity.infrastructure.webhook.controler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.user.identity.infrastructure.webhook.controler.dto.KeycloakDTO;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookKeycloakPayload;

import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KeycloakWebhookControllerTest {

    @Mock
    EventBus eventBus;

    @Mock
    ObjectMapper mapper;

    private KeycloakWebhookController createController() {
        KeycloakWebhookController controller = new KeycloakWebhookController();
        controller.eventBus = eventBus;
        controller.mapper = mapper;
        return controller;
    }

    @Test
    void receiveEvent_shouldSendRegisterEventToEventBus() {
        KeycloakDTO dto = new KeycloakDTO();
        dto.setId(UUID.randomUUID());
        dto.setTime(System.currentTimeMillis());
        dto.setType("REGISTER");
        dto.setRealmId(UUID.randomUUID());
        dto.setClientId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        dto.setUserId(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));

        Response response = createController().receiveEvent(dto, null, null);

        assertEquals(202, response.getStatus());
        verify(eventBus).send(eq("identity.webhook.keycloak"), any(WebhookKeycloakPayload.class));
    }

    @Test
    void receiveEvent_shouldReturnAcceptedForNonRegisterEventType() {
        KeycloakDTO dto = new KeycloakDTO();
        dto.setType("LOGIN");
        dto.setUserId(UUID.randomUUID());
        dto.setClientId(UUID.randomUUID());

        Response response = createController().receiveEvent(dto, null, null);

        assertEquals(202, response.getStatus());
        verify(eventBus, never()).send(anyString(), any());
    }

    @Test
    void receiveEvent_shouldReturnAcceptedForNullDto() {
        Response response = createController().receiveEvent(null, null, null);

        assertEquals(202, response.getStatus());
        verify(eventBus, never()).send(anyString(), any());
    }

    @Test
    void receiveEvent_shouldPassCorrectPayloadToEventBus() {
        UUID userId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
        UUID clientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        KeycloakDTO dto = new KeycloakDTO();
        dto.setType("REGISTER");
        dto.setUserId(userId);
        dto.setClientId(clientId);

        createController().receiveEvent(dto, null, null);

        ArgumentCaptor<WebhookKeycloakPayload> payloadCaptor = ArgumentCaptor.forClass(WebhookKeycloakPayload.class);
        verify(eventBus).send(eq("identity.webhook.keycloak"), payloadCaptor.capture());

        WebhookKeycloakPayload sent = payloadCaptor.getValue();
        assertEquals("REGISTER", sent.eventType());
        assertEquals(userId.toString(), sent.userId());
        assertEquals(clientId.toString(), sent.clientId());
    }

    @Test
    void receiveEvent_shouldDispatchDeleteEventToEventBus() {
        UUID userId = UUID.fromString("660e8400-e29b-41d4-a716-446655440002");
        UUID clientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");

        KeycloakDTO dto = new KeycloakDTO();
        dto.setType("DELETE");
        dto.setUserId(userId);
        dto.setClientId(clientId);

        Response response = createController().receiveEvent(dto, null, null);

        assertEquals(202, response.getStatus());
        ArgumentCaptor<WebhookKeycloakPayload> payloadCaptor = ArgumentCaptor.forClass(WebhookKeycloakPayload.class);
        verify(eventBus).send(eq("identity.webhook.keycloak"), payloadCaptor.capture());

        WebhookKeycloakPayload sent = payloadCaptor.getValue();
        assertEquals("DELETE", sent.eventType());
        assertEquals(userId.toString(), sent.userId());
    }
}
