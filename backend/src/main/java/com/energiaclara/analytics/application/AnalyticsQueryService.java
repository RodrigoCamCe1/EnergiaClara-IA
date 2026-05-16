package com.energiaclara.analytics.application;

import com.energiaclara.analytics.api.dto.AnomalyDto;
import com.energiaclara.analytics.api.dto.DashboardResponse;
import com.energiaclara.analytics.api.dto.KpiSnapshotDto;
import com.energiaclara.energyops.infrastructure.persistence.EnergyAnomalyEntity;
import com.energiaclara.energyops.infrastructure.persistence.EnergyAnomalyRepository;
import com.energiaclara.energyops.infrastructure.persistence.EnergyKpiSnapshotEntity;
import com.energiaclara.energyops.infrastructure.persistence.EnergyKpiSnapshotRepository;
import com.energiaclara.energyops.infrastructure.persistence.EnergyReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AnalyticsQueryService {

    private final EnergyReadingRepository readingRepository;
    private final EnergyKpiSnapshotRepository kpiSnapshotRepository;
    private final EnergyAnomalyRepository anomalyRepository;

    public AnalyticsQueryService(
            EnergyReadingRepository readingRepository,
            EnergyKpiSnapshotRepository kpiSnapshotRepository,
            EnergyAnomalyRepository anomalyRepository
    ) {
        this.readingRepository = readingRepository;
        this.kpiSnapshotRepository = kpiSnapshotRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<KpiSnapshotDto> kpis = kpis();
        List<AnomalyDto> anomalies = anomalies();
        KpiSnapshotDto latest = kpis.isEmpty() ? null : kpis.get(0);
        return new DashboardResponse(
                readingRepository.count(),
                anomalyRepository.count(),
                latest == null ? BigDecimal.ZERO : latest.kwh(),
                latest == null ? BigDecimal.ZERO : latest.deviationPercent(),
                latest != null && latest.anomalyDetected(),
                kpis,
                anomalies
        );
    }

    @Transactional(readOnly = true)
    public List<KpiSnapshotDto> kpis() {
        return kpiSnapshotRepository.findTop20ByOrderByMeasuredAtDesc().stream()
                .map(this::toKpiDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnomalyDto> anomalies() {
        return anomalyRepository.findTop20ByOrderByMeasuredAtDesc().stream()
                .map(this::toAnomalyDto)
                .toList();
    }

    private KpiSnapshotDto toKpiDto(EnergyKpiSnapshotEntity entity) {
        return new KpiSnapshotDto(
                entity.getId(),
                entity.getReadingId(),
                entity.getFacilityId(),
                entity.getMeterId(),
                entity.getMeasuredAt(),
                entity.getKwh(),
                entity.getBaselineKwh(),
                entity.getDeviationPercent(),
                entity.isAnomalyDetected(),
                entity.getEstimatedCostImpact(),
                entity.getEstimatedCo2Impact()
        );
    }

    private AnomalyDto toAnomalyDto(EnergyAnomalyEntity entity) {
        return new AnomalyDto(
                entity.getId(),
                entity.getReadingId(),
                entity.getFacilityId(),
                entity.getMeterId(),
                entity.getMeasuredAt(),
                entity.getType(),
                entity.getSeverity(),
                entity.getDeviationPercent(),
                entity.getExplanation(),
                entity.getRecommendation(),
                entity.getEstimatedCostImpact(),
                entity.getEstimatedCo2Impact()
        );
    }
}
