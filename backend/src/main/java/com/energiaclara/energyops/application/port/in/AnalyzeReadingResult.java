package com.energiaclara.energyops.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record AnalyzeReadingResult(
        boolean anomalyDetected,
        UUID anomalyId,
        String severity,
        BigDecimal deviationPercent,
        BigDecimal estimatedCostImpact,
        BigDecimal estimatedCo2Impact,
        String recommendation
) {
}
