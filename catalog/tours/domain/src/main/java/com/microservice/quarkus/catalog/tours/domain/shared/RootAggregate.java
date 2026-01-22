package com.microservice.quarkus.catalog.tours.domain.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import lombok.NonNull;

public abstract class RootAggregate {

  private final ArrayList<DomainEvent> domainEvents = new ArrayList<>();

  public void registerEvent(@NonNull DomainEvent event) {
    domainEvents.add(event);
  }

  public void clearDomainEvents() {
    domainEvents.clear();
  }

  public Collection<DomainEvent> domainEvents() {
    return Collections.unmodifiableCollection(domainEvents);
  }
}
