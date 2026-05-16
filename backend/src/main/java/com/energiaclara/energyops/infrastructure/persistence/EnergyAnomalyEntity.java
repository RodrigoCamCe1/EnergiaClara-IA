package com.energiaclara.energyops.infrastructure.persistence;

import com.energiaclara.energyops.domain.AnomalySeverity;
import com.energiaclara.energyops.domain.AnomalyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "energy_anomalies")
@Getter
@Setter
@NoArgsConstructor
public class EnergyAnomalyEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AnomalyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnomalySeverity severity;

    @Column(name = "deviation_percent", nullable = false, precision = 9, scale = 3)
    private BigDecimal deviationPercent;

    @Column(nullable = false, length = 700)
    private String explanation;

    @Column(nullable = false, length = 700)
    private String recommendation;

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
