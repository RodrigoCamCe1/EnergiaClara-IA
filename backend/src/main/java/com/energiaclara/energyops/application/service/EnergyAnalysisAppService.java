package com.energiaclara.energyops.application.service;

import com.energiaclara.energyops.application.port.in.AnalyzeReadingCommand;
import com.energiaclara.energyops.application.port.in.AnalyzeReadingResult;
import com.energiaclara.energyops.application.port.in.AnalyzeReadingUseCase;
import com.energiaclara.energyops.domain.AnomalyType;
import com.energiaclara.energyops.domain.exception.NoAnomalyDetectedException;
import com.energiaclara.energyops.domain.model.Anomaly;
import com.energiaclara.energyops.domain.port.out.AnomalyRepositoryPort;
import com.energiaclara.energyops.domain.port.out.DomainEventPublisherPort;
import com.energiaclara.energyops.domain.service.AnomalyDetectionDomainService;
import com.energiaclara.energyops.domain.vo.AnomalyScope;
import com.energiaclara.energyops.domain.vo.DeviationPercent;
import com.energiaclara.energyops.domain.vo.EnergyValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class EnergyAnalysisAppService implements AnalyzeReadingUseCase {
    private final AnomalyRepositoryPort anomalyRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final AnomalyDetectionDomainService detectionService = new AnomalyDetectionDomainService();

    public EnergyAnalysisAppService(
            AnomalyRepositoryPort anomalyRepository,
            DomainEventPublisherPort eventPublisher
    ) {
        this.anomalyRepository = anomalyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AnalyzeReadingResult analyze(AnalyzeReadingCommand command) {
        AnomalyScope scope = AnomalyScope.of(command.facilityId(), command.meterId());
        EnergyValue actual = EnergyValue.of(command.kwh());
        EnergyValue baseline = EnergyValue.of(command.baselineKwh());

        try {
            Anomaly anomaly = detectionService.detectAnomaly(
                    scope,
                    AnomalyType.EXCESS_CONSUMPTION,
                    actual,
                    baseline,
                    command.tolerancePercent(),
                    command.costPerKwh(),
                    command.co2KgPerKwh()
            );
            Anomaly saved = anomalyRepository.save(anomaly);
            eventPublisher.publishAll(saved.pullDomainEvents());
            return new AnalyzeReadingResult(
                    true,
                    saved.getId().value(),
                    saved.getSeverity().level().name(),
                    saved.getDeviationPercent().value(),
                    saved.getImpact().costImpact(),
                    saved.getImpact().co2Impact(),
                    saved.getRecommendation()
            );
        } catch (NoAnomalyDetectedException ex) {
            DeviationPercent deviation = DeviationPercent.calculate(actual, baseline);
            return new AnalyzeReadingResult(
                    false,
                    null,
                    null,
                    deviation.value(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "Consumo dentro del rango esperado del baseline."
            );
        }
    }
}
