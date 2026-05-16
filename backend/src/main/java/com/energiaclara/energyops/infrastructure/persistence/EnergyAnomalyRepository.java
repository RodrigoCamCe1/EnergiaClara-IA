package com.energiaclara.energyops.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnergyAnomalyRepository extends JpaRepository<EnergyAnomalyEntity, UUID> {
    List<EnergyAnomalyEntity> findByFacilityIdAndMeterId(String facilityId, String meterId);
    List<EnergyAnomalyEntity> findTop20ByOrderByMeasuredAtDesc();
}
