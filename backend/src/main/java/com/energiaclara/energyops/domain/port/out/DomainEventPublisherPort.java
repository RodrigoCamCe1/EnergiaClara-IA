package com.energiaclara.energyops.domain.port.out;

import com.energiaclara.energyops.domain.event.DomainEvent;

import java.util.List;

public interface DomainEventPublisherPort {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
