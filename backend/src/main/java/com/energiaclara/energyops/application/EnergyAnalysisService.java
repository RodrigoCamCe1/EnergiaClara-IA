package com.energiaclara.energyops.application;

import com.energiaclara.energyops.api.dto.AnalyzeReadingRequest;
import com.energiaclara.energyops.api.dto.AnalyzeReadingResponse;
import com.energiaclara.energyops.domain.AnomalySeverity;
import com.energiaclara.energyops.domain.AnomalyType;
import com.energiaclara.energyops.infrastructure.persistence.EnergyAnomalyEntity;
import com.energiaclara.energyops.infrastructure.persistence.EnergyAnomalyRepository;
import com.energiaclara.energyops.infrastructure.persistence.EnergyBaselineRepository;
import com.energiaclara.energyops.infrastructure.persistence.EnergyKpiSnapshotEntity;
import com.energiaclara.energyops.infrastructure.persistence.EnergyKpiSnapshotRepository;
import com.energiaclara.energyops.infrastructure.persistence.EnergyReadingEntity;
import com.energiaclara.energyops.infrastructure.persistence.EnergyReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EnergyAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EnergyAnalysisService.class);

    private final EnergyReadingRepository readingRepository;
    private final EnergyBaselineRepository baselineRepository;
    private final EnergyAnomalyRepository anomalyRepository;
    private final EnergyKpiSnapshotRepository kpiSnapshotRepository;
    private final BigDecimal demoDefaultBaselineKwh;
    private final BigDecimal demoDefaultTolerancePercent;
    private final BigDecimal costPerKwh;
    private final BigDecimal co2KgPerKwh;

    public EnergyAnalysisService(
            EnergyReadingRepository readingRepository,
            EnergyBaselineRepository baselineRepository,
            EnergyAnomalyRepository anomalyRepository,
            EnergyKpiSnapshotRepository kpiSnapshotRepository,
            @Value("${app.energyops.demo-default-baseline-kwh:100}") BigDecimal demoDefaultBaselineKwh,
            @Value("${app.energyops.demo-default-tolerance-percent:15}") BigDecimal demoDefaultTolerancePercent,
            @Value("${app.energyops.cost-per-kwh:1.50625}") BigDecimal costPerKwh,
            @Value("${app.energyops.co2-kg-per-kwh:0.44}") BigDecimal co2KgPerKwh
    ) {
        this.readingRepository = readingRepository;
        this.baselineRepository = baselineRepository;
        this.anomalyRepository = anomalyRepository;
        this.kpiSnapshotRepository = kpiSnapshotRepository;
        this.demoDefaultBaselineKwh = demoDefaultBaselineKwh;
        this.demoDefaultTolerancePercent = demoDefaultTolerancePercent;
        this.costPerKwh = costPerKwh;
        this.co2KgPerKwh = co2KgPerKwh;
    }

    @Transactional
    public AnalyzeReadingResponse analyze(AnalyzeReadingRequest request) {
        log.info("Lectura energetica recibida facilityId={} meterId={} measuredAt={} kwh={}",
                request.facilityId(), request.meterId(), request.measuredAt(), request.kwh());

        EnergyBaseline baseline = resolveBaseline(request.facilityId(), request.meterId());

        EnergyReadingEntity reading = new EnergyReadingEntity();
        reading.setFacilityId(request.facilityId());
        reading.setMeterId(request.meterId());
        reading.setMeasuredAt(request.measuredAt());
        reading.setKwh(request.kwh());
        reading.setVoltage(request.voltage());
        reading.setPowerFactor(request.powerFactor());
        reading = readingRepository.save(reading);

        BigDecimal deviationPercent = calculateDeviationPercent(request.kwh(), baseline.expectedKwh());
        boolean anomalyDetected = deviationPercent.compareTo(baseline.tolerancePercent()) > 0;
        BigDecimal excessKwh = request.kwh().subtract(baseline.expectedKwh()).max(BigDecimal.ZERO);
        BigDecimal estimatedCostImpact = money(excessKwh.multiply(costPerKwh));
        BigDecimal estimatedCo2Impact = money(excessKwh.multiply(co2KgPerKwh));

        EnergyAnomalyEntity anomaly = null;
        AnomalySeverity severity = null;
        String recommendation = "Consumo dentro del rango esperado del baseline.";

        if (anomalyDetected) {
            severity = severityFor(deviationPercent);
            recommendation = "Revisar equipos activos fuera de horario o consumo superior al baseline.";
            anomaly = buildAnomaly(reading, deviationPercent, severity, recommendation, estimatedCostImpact, estimatedCo2Impact);
            anomaly = anomalyRepository.save(anomaly);
            log.info("Anomalia detectada readingId={} anomalyId={} severity={} deviationPercent={}",
                    reading.getId(), anomaly.getId(), severity, deviationPercent);
        } else {
            log.info("Sin anomalia readingId={} deviationPercent={} tolerancePercent={}",
                    reading.getId(), deviationPercent, baseline.tolerancePercent());
        }

        EnergyKpiSnapshotEntity kpi = buildKpi(reading, baseline.expectedKwh(), deviationPercent,
                anomalyDetected, estimatedCostImpact, estimatedCo2Impact);
        kpiSnapshotRepository.save(kpi);
        log.info("KPI generado readingId={} kpiId={} anomalyDetected={}", reading.getId(), kpi.getId(), anomalyDetected);

        return new AnalyzeReadingResponse(
                reading.getId(),
                anomaly == null ? null : anomaly.getId(),
                anomalyDetected,
                severity,
                percent(deviationPercent),
                recommendation,
                estimatedCostImpact,
                estimatedCo2Impact
        );
    }

    private EnergyBaseline resolveBaseline(String facilityId, String meterId) {
        return baselineRepository.findFirstByFacilityIdAndMeterIdAndActiveTrue(facilityId, meterId)
                .map(entity -> {
                    log.info("Baseline encontrado facilityId={} meterId={} expectedKwh={} tolerancePercent={}",
                            facilityId, meterId, entity.getExpectedKwh(), entity.getTolerancePercent());
                    return new EnergyBaseline(entity.getExpectedKwh(), entity.getTolerancePercent());
                })
                .orElseGet(() -> {
                    log.warn("No existe baseline activo para facilityId={} meterId={}; usando fallback demo expectedKwh={} tolerancePercent={}",
                            facilityId, meterId, demoDefaultBaselineKwh, demoDefaultTolerancePercent);
                    return new EnergyBaseline(demoDefaultBaselineKwh, demoDefaultTolerancePercent);
                });
    }

    private BigDecimal calculateDeviationPercent(BigDecimal kwh, BigDecimal expectedKwh) {
        if (expectedKwh == null || expectedKwh.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Baseline invalido: expectedKwh debe ser mayor a 0");
        }
        return kwh.subtract(expectedKwh)
                .divide(expectedKwh, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private AnomalySeverity severityFor(BigDecimal deviationPercent) {
        if (deviationPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return AnomalySeverity.CRITICAL;
        }
        if (deviationPercent.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return AnomalySeverity.HIGH;
        }
        if (deviationPercent.compareTo(BigDecimal.valueOf(25)) >= 0) {
            return AnomalySeverity.MEDIUM;
        }
        return AnomalySeverity.LOW;
    }

    private EnergyAnomalyEntity buildAnomaly(
            EnergyReadingEntity reading,
            BigDecimal deviationPercent,
            AnomalySeverity severity,
            String recommendation,
            BigDecimal estimatedCostImpact,
            BigDecimal estimatedCo2Impact
    ) {
        EnergyAnomalyEntity anomaly = new EnergyAnomalyEntity();
        anomaly.setReadingId(reading.getId());
        anomaly.setFacilityId(reading.getFacilityId());
        anomaly.setMeterId(reading.getMeterId());
        anomaly.setMeasuredAt(reading.getMeasuredAt());
        anomaly.setType(AnomalyType.EXCESS_CONSUMPTION);
        anomaly.setSeverity(severity);
        anomaly.setDeviationPercent(percent(deviationPercent));
        anomaly.setExplanation("La lectura supera el baseline configurado para el medidor.");
        anomaly.setRecommendation(recommendation);
        anomaly.setEstimatedCostImpact(estimatedCostImpact);
        anomaly.setEstimatedCo2Impact(estimatedCo2Impact);
        return anomaly;
    }

    private EnergyKpiSnapshotEntity buildKpi(
            EnergyReadingEntity reading,
            BigDecimal baselineKwh,
            BigDecimal deviationPercent,
            boolean anomalyDetected,
            BigDecimal estimatedCostImpact,
            BigDecimal estimatedCo2Impact
    ) {
        EnergyKpiSnapshotEntity kpi = new EnergyKpiSnapshotEntity();
        kpi.setReadingId(reading.getId());
        kpi.setFacilityId(reading.getFacilityId());
        kpi.setMeterId(reading.getMeterId());
        kpi.setMeasuredAt(reading.getMeasuredAt());
        kpi.setKwh(reading.getKwh());
        kpi.setBaselineKwh(baselineKwh);
        kpi.setDeviationPercent(percent(deviationPercent));
        kpi.setAnomalyDetected(anomalyDetected);
        kpi.setEstimatedCostImpact(estimatedCostImpact);
        kpi.setEstimatedCo2Impact(estimatedCo2Impact);
        return kpi;
    }

    private BigDecimal percent(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record EnergyBaseline(BigDecimal expectedKwh, BigDecimal tolerancePercent) {
    }
}
