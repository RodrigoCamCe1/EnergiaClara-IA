package com.energiaclara.energyops.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnergyKpiSnapshotRepository extends JpaRepository<EnergyKpiSnapshotEntity, UUID> {
    List<EnergyKpiSnapshotEntity> findTop20ByOrderByMeasuredAtDesc();
}
