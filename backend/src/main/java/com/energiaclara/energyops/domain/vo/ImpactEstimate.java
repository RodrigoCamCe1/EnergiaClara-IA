package com.energiaclara.energyops.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ImpactEstimate(BigDecimal costImpact, BigDecimal co2Impact) {
    public static ImpactEstimate calculate(EnergyValue excess, BigDecimal costPerKwh, BigDecimal co2KgPerKwh) {
        BigDecimal cost = excess.kwh().multiply(costPerKwh).setScale(2, RoundingMode.HALF_UP);
        BigDecimal co2 = excess.kwh().multiply(co2KgPerKwh).setScale(2, RoundingMode.HALF_UP);
        return new ImpactEstimate(cost, co2);
    }
}
