package com.energiaclara.energyops.domain.service;

import com.energiaclara.energyops.domain.AnomalyType;
import com.energiaclara.energyops.domain.exception.NoAnomalyDetectedException;
import com.energiaclara.energyops.domain.model.Anomaly;
import com.energiaclara.energyops.domain.vo.AnomalyScope;
import com.energiaclara.energyops.domain.vo.DeviationPercent;
import com.energiaclara.energyops.domain.vo.EnergyValue;
import com.energiaclara.energyops.domain.vo.ImpactEstimate;

import java.math.BigDecimal;

public class AnomalyDetectionDomainService {
    public Anomaly detectAnomaly(
            AnomalyScope scope,
            AnomalyType type,
            EnergyValue actual,
            EnergyValue baseline,
            BigDecimal tolerancePercent,
            BigDecimal costPerKwh,
            BigDecimal co2KgPerKwh
    ) {
        DeviationPercent deviation = DeviationPercent.calculate(actual, baseline);
        if (!deviation.isAboveThreshold(tolerancePercent)) {
            throw new NoAnomalyDetectedException(scope.facilityId(), scope.meterId(), deviation.value());
        }
        EnergyValue excess = actual.excessOver(baseline);
        ImpactEstimate impact = ImpactEstimate.calculate(excess, costPerKwh, co2KgPerKwh);
        return Anomaly.detect(
                scope,
                type,
                deviation,
                impact,
                "La lectura supera el baseline configurado para el medidor.",
                "Revisar equipos activos fuera de horario o consumo superior al baseline."
        );
    }
}
