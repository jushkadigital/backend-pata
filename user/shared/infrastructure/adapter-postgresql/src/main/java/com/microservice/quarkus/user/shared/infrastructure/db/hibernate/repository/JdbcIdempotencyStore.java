package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.shared.application.outbox.IdempotencyStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

@ApplicationScoped
public class JdbcIdempotencyStore implements IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcIdempotencyStore.class);
    private static final long TTL_HOURS = 24;

    @Inject
    DataSource dataSource;

    @Override
    @Transactional
    public boolean tryAcquire(String eventId, String consumerGroup) {
        try (Connection conn = dataSource.getConnection()) {
            // Clean expired entries first
            try (PreparedStatement cleanup = conn.prepareStatement(
                    "DELETE FROM quarkus.event_idempotency WHERE expires_at IS NOT NULL AND expires_at < ?")) {
                cleanup.setTimestamp(1, Timestamp.from(Instant.now()));
                cleanup.executeUpdate();
            }

            // Try to insert - uses ON CONFLICT DO NOTHING for atomicity
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO quarkus.event_idempotency (event_id, consumer_group, created_at, expires_at) " +
                    "VALUES (?, ?, ?, ?) ON CONFLICT (event_id, consumer_group) DO NOTHING")) {
                insert.setString(1, eventId);
                insert.setString(2, consumerGroup);
                insert.setTimestamp(3, Timestamp.from(Instant.now()));
                insert.setTimestamp(4, Timestamp.from(Instant.now().plusSeconds(TTL_HOURS * 3600)));
                int rows = insert.executeUpdate();
                if (rows > 0) {
                    log.debug("Idempotency acquired: eventId={}, consumerGroup={}", eventId, consumerGroup);
                    return true;
                } else {
                    log.info("Idempotent skip — already processed: eventId={}, consumerGroup={}", eventId, consumerGroup);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Idempotency check failed, allowing processing: eventId={}, consumerGroup={}", eventId, consumerGroup, e);
            // Fail-open: if store is unavailable, allow processing (better than blocking)
            return true;
        }
    }
}
