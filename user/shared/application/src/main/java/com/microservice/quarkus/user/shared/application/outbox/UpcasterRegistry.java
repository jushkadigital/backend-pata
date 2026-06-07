package com.microservice.quarkus.user.shared.application.outbox;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;
import java.util.*;

public class UpcasterRegistry {

    private final Map<String, List<EventUpcaster>> upcasters = new LinkedHashMap<>();

    public void register(EventUpcaster upcaster) {
        upcasters.computeIfAbsent(upcaster.sourceEventType(), k -> new ArrayList<>()).add(upcaster);
    }

    public IntegrationEvent upcast(IntegrationEvent event) {
        List<EventUpcaster> chain = upcasters.getOrDefault(event.eventType(), List.of());
        IntegrationEvent current = event;
        for (EventUpcaster upcaster : chain) {
            if (current.eventVersion() == upcaster.sourceVersion()) {
                current = upcaster.upcast(current);
            }
        }
        return current;
    }

    public boolean hasUpcasters(String eventType) {
        return upcasters.containsKey(eventType) && !upcasters.get(eventType).isEmpty();
    }
}
