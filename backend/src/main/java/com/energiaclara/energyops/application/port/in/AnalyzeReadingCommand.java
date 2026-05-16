package com.energiaclara.energyops.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyzeReadingCommand(
        String facilityId,
        String meterId,
        BigDecimal kwh,
        BigDecimal baselineKwh,
        BigDecimal tolerancePercent,
        BigDecimal costPerKwh,
        BigDecimal co2KgPerKwh,
        Instant measuredAt
) {
}
