package com.energiaclara.energyops.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnergyValueTest {

    @Test
    void negativeKwhThrows() {
        assertThrows(IllegalArgumentException.class, () -> new EnergyValue(BigDecimal.valueOf(-1)));
    }

    @Test
    void excessOverReturnsZeroWhenActualBelowBaseline() {
        EnergyValue actual = new EnergyValue(BigDecimal.valueOf(80));
        EnergyValue baseline = new EnergyValue(BigDecimal.valueOf(100));

        EnergyValue excess = actual.excessOver(baseline);

        assertEquals(BigDecimal.ZERO, excess.kwh());
    }

    @Test
    void excessOverReturnsDifferenceWhenActualAboveBaseline() {
        EnergyValue actual = new EnergyValue(BigDecimal.valueOf(180));
        EnergyValue baseline = new EnergyValue(BigDecimal.valueOf(100));

        EnergyValue excess = actual.excessOver(baseline);

        assertEquals(BigDecimal.valueOf(80), excess.kwh());
    }
}
