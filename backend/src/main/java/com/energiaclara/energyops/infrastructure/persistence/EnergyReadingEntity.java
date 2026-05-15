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
@Table(name = "energy_readings")
@Getter
@Setter
@NoArgsConstructor
public class EnergyReadingEntity {

    @Id
    private UUID id;

    @Column(name = "facility_id", nullable = false, length = 80)
    private String facilityId;

    @Column(name = "meter_id", nullable = false, length = 80)
    private String meterId;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal kwh;

    @Column(precision = 10, scale = 3)
    private BigDecimal voltage;

    @Column(name = "power_factor", precision = 5, scale = 3)
    private BigDecimal powerFactor;

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
