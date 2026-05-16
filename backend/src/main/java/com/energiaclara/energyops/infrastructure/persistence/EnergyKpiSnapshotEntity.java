package com.energiaclara.energyops.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "energy_kpi_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class EnergyKpiSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "reading_id", nullable = false)
    private UUID readingId;

    @Column(name = "facility_id", nullable = false, length = 80)
    private String facilityId;

    @Column(name = "meter_id", nullable = false, length = 80)
    private String meterId;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal kwh;

    @Column(name = "baseline_kwh", nullable = false, precision = 12, scale = 3)
    private BigDecimal baselineKwh;

    @Column(name = "deviation_percent", nullable = false, precision = 9, scale = 3)
    private BigDecimal deviationPercent;

    @Column(name = "anomaly_detected", nullable = false)
    private boolean anomalyDetected;

    @Column(name = "estimated_cost_impact", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedCostImpact;

    @Column(name = "estimated_co2_impact", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedCo2Impact;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
