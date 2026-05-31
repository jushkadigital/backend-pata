package com.microservice.quarkus.user.identity.infrastructure.keycloak;

import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Mock
    Keycloak keycloak;

    @Mock
    RealmResource realmResource;

    @Mock
    UsersResource usersResource;

    @Mock
    UserResource userResource;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private KeycloakService createService() {
        KeycloakService service = new KeycloakService();
        service.meterRegistry = meterRegistry;
        service.init();
        service.keycloak = keycloak;
        return service;
    }

    @Test
    void deleteUser_shouldCallKeycloakDelete() {
        when(keycloak.realm("quarkus")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-123")).thenReturn(userResource);

        KeycloakService service = createService();

        service.deleteUser("user-123");

        verify(userResource).remove();
    }

    @Test
    void deleteUser_shouldNotPropagateExceptionWhenKeycloakFails() {
        when(keycloak.realm("quarkus")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-456")).thenReturn(userResource);
        doThrow(new RuntimeException("Keycloak unavailable")).when(userResource).remove();

        KeycloakService service = createService();

        service.deleteUser("user-456");

        verify(userResource).remove();
    }
}
