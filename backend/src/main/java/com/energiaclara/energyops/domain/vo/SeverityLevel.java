package com.energiaclara.energyops.domain.vo;

import com.energiaclara.energyops.domain.AnomalySeverity;

import java.math.BigDecimal;

public record SeverityLevel(AnomalySeverity level) {
    public static SeverityLevel from(DeviationPercent deviation) {
        BigDecimal value = deviation.value();
        if (value.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return new SeverityLevel(AnomalySeverity.CRITICAL);
        }
        if (value.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return new SeverityLevel(AnomalySeverity.HIGH);
        }
        if (value.compareTo(BigDecimal.valueOf(25)) >= 0) {
            return new SeverityLevel(AnomalySeverity.MEDIUM);
        }
        return new SeverityLevel(AnomalySeverity.LOW);
    }
}
