package com.energiaclara.energyops.domain.model;

import java.util.UUID;

public record AnomalyId(UUID value) {
    public static AnomalyId generate() {
        return new AnomalyId(UUID.randomUUID());
    }

    public static AnomalyId of(UUID value) {
        return new AnomalyId(value);
    }

    public static AnomalyId of(String value) {
        return new AnomalyId(UUID.fromString(value));
    }
}
