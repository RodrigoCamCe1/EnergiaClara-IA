package com.energiaclara.energyops.domain.port.out;

import com.energiaclara.energyops.domain.model.Anomaly;
import com.energiaclara.energyops.domain.model.AnomalyId;
import com.energiaclara.energyops.domain.vo.AnomalyScope;

import java.util.List;
import java.util.Optional;

public interface AnomalyRepositoryPort {
    Anomaly save(Anomaly anomaly);
    Optional<Anomaly> findById(AnomalyId id);
    List<Anomaly> findByScope(AnomalyScope scope);
}
