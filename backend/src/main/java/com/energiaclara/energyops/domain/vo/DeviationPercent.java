package com.energiaclara.energyops.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record DeviationPercent(BigDecimal value) {
    public static DeviationPercent calculate(EnergyValue actual, EnergyValue baseline) {
        if (baseline.kwh().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Baseline debe ser mayor a cero");
        }
        BigDecimal deviation = actual.kwh()
                .subtract(baseline.kwh())
                .divide(baseline.kwh(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return new DeviationPercent(deviation);
    }

    public boolean isAboveThreshold(BigDecimal threshold) {
        return value.compareTo(threshold) > 0;
    }
}
