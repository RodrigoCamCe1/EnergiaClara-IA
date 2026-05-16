package com.energiaclara.energyops.domain.vo;

import com.energiaclara.energyops.domain.AnomalySeverity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeverityLevelTest {

    @Test
    void deviation100IsCritical() {
        SeverityLevel level = SeverityLevel.from(new DeviationPercent(BigDecimal.valueOf(100)));

        assertEquals(AnomalySeverity.CRITICAL, level.level());
    }

    @Test
    void deviation60IsHigh() {
        SeverityLevel level = SeverityLevel.from(new DeviationPercent(BigDecimal.valueOf(60)));

        assertEquals(AnomalySeverity.HIGH, level.level());
    }

    @Test
    void deviation30IsMedium() {
        SeverityLevel level = SeverityLevel.from(new DeviationPercent(BigDecimal.valueOf(30)));

        assertEquals(AnomalySeverity.MEDIUM, level.level());
    }

    @Test
    void deviation10IsLow() {
        SeverityLevel level = SeverityLevel.from(new DeviationPercent(BigDecimal.valueOf(10)));

        assertEquals(AnomalySeverity.LOW, level.level());
    }
}
