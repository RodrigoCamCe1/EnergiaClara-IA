package com.energiaclara.energyops.infrastructure.persistence;

import com.energiaclara.energyops.domain.model.Anomaly;
import com.energiaclara.energyops.domain.model.AnomalyId;
import com.energiaclara.energyops.domain.model.AnomalyStatus;
import com.energiaclara.energyops.domain.port.out.AnomalyRepositoryPort;
import com.energiaclara.energyops.domain.vo.AnomalyScope;
import com.energiaclara.energyops.domain.vo.DeviationPercent;
import com.energiaclara.energyops.domain.vo.ImpactEstimate;
import com.energiaclara.energyops.domain.vo.SeverityLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AnomalyRepositoryAdapter implements AnomalyRepositoryPort {
    private final EnergyAnomalyRepository repository;

    public AnomalyRepositoryAdapter(EnergyAnomalyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Anomaly save(Anomaly anomaly) {
        EnergyAnomalyEntity entity = toEntity(anomaly);
        EnergyAnomalyEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Anomaly> findById(AnomalyId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Anomaly> findByScope(AnomalyScope scope) {
        return repository.findByFacilityIdAndMeterId(scope.facilityId(), scope.meterId())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private EnergyAnomalyEntity toEntity(Anomaly anomaly) {
        EnergyAnomalyEntity entity = new EnergyAnomalyEntity();
        entity.setId(anomaly.getId().value());
        entity.setReadingId(null);
        entity.setFacilityId(anomaly.getScope().facilityId());
        entity.setMeterId(anomaly.getScope().meterId());
        entity.setMeasuredAt(anomaly.getDetectedAt());
        entity.setType(anomaly.getType());
        entity.setSeverity(anomaly.getSeverity().level());
        entity.setDeviationPercent(anomaly.getDeviationPercent().value());
        entity.setExplanation(anomaly.getExplanation());
        entity.setRecommendation(anomaly.getRecommendation());
        entity.setEstimatedCostImpact(anomaly.getImpact().costImpact());
        entity.setEstimatedCo2Impact(anomaly.getImpact().co2Impact());
        return entity;
    }

    private Anomaly toDomain(EnergyAnomalyEntity entity) {
        return Anomaly.reconstruct(
                AnomalyId.of(entity.getId()),
                AnomalyScope.of(entity.getFacilityId(), entity.getMeterId()),
                entity.getType(),
                new SeverityLevel(entity.getSeverity()),
                new DeviationPercent(entity.getDeviationPercent()),
                new ImpactEstimate(entity.getEstimatedCostImpact(), entity.getEstimatedCo2Impact()),
                entity.getExplanation(),
                entity.getRecommendation(),
                AnomalyStatus.DETECTED,
                entity.getMeasuredAt()
        );
    }
}
