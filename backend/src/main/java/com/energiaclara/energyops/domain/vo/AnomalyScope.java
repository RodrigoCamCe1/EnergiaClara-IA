package com.energiaclara.energyops.domain.vo;

public record AnomalyScope(String facilityId, String meterId) {
    public AnomalyScope {
        if (facilityId == null || facilityId.isBlank() || meterId == null || meterId.isBlank()) {
            throw new IllegalArgumentException("facilityId y meterId son obligatorios");
        }
    }

    public static AnomalyScope of(String facilityId, String meterId) {
        return new AnomalyScope(facilityId, meterId);
    }
}
