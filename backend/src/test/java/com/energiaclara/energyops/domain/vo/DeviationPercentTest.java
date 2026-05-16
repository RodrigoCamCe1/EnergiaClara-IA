package com.energiaclara.energyops.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviationPercentTest {

    @Test
    void baselineZeroThrows() {
        EnergyValue actual = new EnergyValue(BigDecimal.valueOf(100));
        EnergyValue baseline = new EnergyValue(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> DeviationPercent.calculate(actual, baseline));
    }

    @Test
    void calculatesPositiveDeviation() {
        EnergyValue actual = new EnergyValue(BigDecimal.valueOf(180));
        EnergyValue baseline = new EnergyValue(BigDecimal.valueOf(100));

        DeviationPercent deviation = DeviationPercent.calculate(actual, baseline);

        assertEquals(new BigDecimal("80.000000"), deviation.value());
    }

    @Test
    void calculatesNegativeDeviation() {
        EnergyValue actual = new EnergyValue(BigDecimal.valueOf(80));
        EnergyValue baseline = new EnergyValue(BigDecimal.valueOf(100));

        DeviationPercent deviation = DeviationPercent.calculate(actual, baseline);

        assertEquals(new BigDecimal("-20.000000"), deviation.value());
    }
}
