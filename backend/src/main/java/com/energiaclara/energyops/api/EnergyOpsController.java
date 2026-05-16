package com.energiaclara.energyops.api;

import com.energiaclara.energyops.api.dto.AnalyzeReadingRequest;
import com.energiaclara.energyops.api.dto.AnalyzeReadingResponse;
import com.energiaclara.energyops.application.port.in.AnalyzeReadingCommand;
import com.energiaclara.energyops.application.port.in.AnalyzeReadingResult;
import com.energiaclara.energyops.application.port.in.AnalyzeReadingUseCase;
import com.energiaclara.energyops.domain.AnomalySeverity;
import com.energiaclara.energyops.infrastructure.persistence.EnergyBaselineRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/energyops")
public class EnergyOpsController {

    private final AnalyzeReadingUseCase analyzeReadingUseCase;
    private final EnergyBaselineRepository baselineRepository;
    private final BigDecimal demoDefaultBaselineKwh;
    private final BigDecimal demoDefaultTolerancePercent;
    private final BigDecimal costPerKwh;
    private final BigDecimal co2KgPerKwh;

    public EnergyOpsController(
            AnalyzeReadingUseCase analyzeReadingUseCase,
            EnergyBaselineRepository baselineRepository,
            @Value("${app.energyops.demo-default-baseline-kwh:100}") BigDecimal demoDefaultBaselineKwh,
            @Value("${app.energyops.demo-default-tolerance-percent:15}") BigDecimal demoDefaultTolerancePercent,
            @Value("${app.energyops.cost-per-kwh:1.50625}") BigDecimal costPerKwh,
            @Value("${app.energyops.co2-kg-per-kwh:0.44}") BigDecimal co2KgPerKwh
    ) {
        this.analyzeReadingUseCase = analyzeReadingUseCase;
        this.baselineRepository = baselineRepository;
        this.demoDefaultBaselineKwh = demoDefaultBaselineKwh;
        this.demoDefaultTolerancePercent = demoDefaultTolerancePercent;
        this.costPerKwh = costPerKwh;
        this.co2KgPerKwh = co2KgPerKwh;
    }

    @PostMapping("/analyze-reading")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalyzeReadingResponse analyzeReading(@Valid @RequestBody AnalyzeReadingRequest request) {
        EnergyBaseline baseline = resolveBaseline(request.facilityId(), request.meterId());
        AnalyzeReadingCommand command = new AnalyzeReadingCommand(
                request.facilityId(),
                request.meterId(),
                request.kwh(),
                baseline.expectedKwh(),
                baseline.tolerancePercent(),
                costPerKwh,
                co2KgPerKwh,
                request.measuredAt()
        );
        AnalyzeReadingResult result = analyzeReadingUseCase.analyze(command);
        return new AnalyzeReadingResponse(
                null,
                result.anomalyId(),
                result.anomalyDetected(),
                result.severity() == null ? null : AnomalySeverity.valueOf(result.severity()),
                result.deviationPercent(),
                result.recommendation(),
                result.estimatedCostImpact(),
                result.estimatedCo2Impact()
        );
    }

    private EnergyBaseline resolveBaseline(String facilityId, String meterId) {
        return baselineRepository.findFirstByFacilityIdAndMeterIdAndActiveTrue(facilityId, meterId)
                .map(entity -> new EnergyBaseline(entity.getExpectedKwh(), entity.getTolerancePercent()))
                .orElseGet(() -> new EnergyBaseline(demoDefaultBaselineKwh, demoDefaultTolerancePercent));
    }

    private record EnergyBaseline(BigDecimal expectedKwh, BigDecimal tolerancePercent) {
    }
}
