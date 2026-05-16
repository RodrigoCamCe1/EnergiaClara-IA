package com.energiaclara.energyops.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
    String eventType();
}
