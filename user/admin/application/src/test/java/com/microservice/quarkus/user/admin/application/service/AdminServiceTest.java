package com.microservice.quarkus.user.admin.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.repository.AdminPanacheRepository;
import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    AdminPanacheRepository repository;

    @Mock
    OutboxEventRepository outboxEventRepository;

    @Mock
    TraceContextProvider traceContextProvider;

    @Mock
    AdminMetrics adminMetrics;

    private AdminService createService() {
        lenient().when(traceContextProvider.current()).thenReturn(EventMetadata.empty());
        return new AdminService(repository, outboxEventRepository, traceContextProvider, adminMetrics);
    }

    @Test
    void register_shouldCreateAdminWithCorrectFields() {
        AdminService service = createService();
        CreateAdminCommand cmd = new CreateAdminCommand("ext-admin-1", "admin@example.com", "ADMIN");

        AtomicReference<AdminEntity> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(repository).persist(any(AdminEntity.class));

        String id = service.register(cmd);

        assertNotNull(id);
        AdminEntity entity = captured.get();
        assertNotNull(entity);
        assertEquals("ext-admin-1", entity.getExternalId());
        assertEquals(new EmailAddress("admin@example.com"), entity.getEmail());
        assertEquals("ADMIN", entity.getType());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void register_shouldCreateOutboxEvent() {
        AdminService service = createService();
        CreateAdminCommand cmd = new CreateAdminCommand("ext-admin-2", "super@example.com", "SUPER_ADMIN");

        service.register(cmd);

        verify(outboxEventRepository, times(2)).save(argThat(event ->
                ("admin.registered.v1".equals(event.getEventType())
                        || "notification.admin.registered.v1".equals(event.getEventType()))
                        && "Admin".equals(event.getAggregateType())
                        && !event.getPublished()));
    }

    @Test
    void register_shouldSetSuperAdminType() {
        AdminService service = createService();
        CreateAdminCommand cmd = new CreateAdminCommand("ext-admin-3", "superadmin@example.com", "SUPER_ADMIN");

        AtomicReference<AdminEntity> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(repository).persist(any(AdminEntity.class));

        service.register(cmd);

        assertEquals("SUPER_ADMIN", captured.get().getType());
    }

    @Test
    void register_shouldSetEditorType() {
        AdminService service = createService();
        CreateAdminCommand cmd = new CreateAdminCommand("ext-admin-4", "editor@example.com", "EDITOR");

        AtomicReference<AdminEntity> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(repository).persist(any(AdminEntity.class));

        service.register(cmd);

        assertEquals("EDITOR", captured.get().getType());
    }

    @Test
    void register_shouldReturnGeneratedId() {
        AdminService service = createService();
        CreateAdminCommand cmd = new CreateAdminCommand("ext-admin-5", "newadmin@example.com", "ADMIN");

        AtomicReference<AdminEntity> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(repository).persist(any(AdminEntity.class));

        String id = service.register(cmd);

        assertNotNull(id);
        assertFalse(id.isEmpty());
        assertEquals(captured.get().getId(), id);
    }
}
