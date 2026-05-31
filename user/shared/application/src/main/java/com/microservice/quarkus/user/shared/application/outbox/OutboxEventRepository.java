package com.microservice.quarkus.user.shared.application.outbox;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {
  void save(OutboxEvent event);

  void update(OutboxEvent event);

  List<OutboxEvent> findUnpublished();

  List<OutboxEvent> findDeadEvents();

  void deletePublishedBefore(Instant threshold);
}
