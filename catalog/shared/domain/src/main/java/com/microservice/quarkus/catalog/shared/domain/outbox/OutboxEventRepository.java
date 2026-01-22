package com.microservice.quarkus.catalog.shared.domain.outbox;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {

    void save(OutboxEvent event);

    void update(OutboxEvent event);

    List<OutboxEvent> findUnpublished();

    List<OutboxEvent> findUnpublishedBySubdomain(String subdomain);

    void deletePublishedBefore(Instant threshold);
}
