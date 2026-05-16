package com.energiaclara.energyops.domain.event;

import com.energiaclara.energyops.domain.model.AnomalyId;
import com.energiaclara.energyops.domain.vo.AnomalyScope;
import com.energiaclara.energyops.domain.vo.SeverityLevel;

import java.time.Instant;

public record AnomalyDetectedEvent(
        AnomalyId anomalyId,
        AnomalyScope scope,
        SeverityLevel severity,
        Instant occurredOn
) implements DomainEvent {
    @Override
    public String eventType() {
        return "energyops.anomaly.detected";
    }
}
