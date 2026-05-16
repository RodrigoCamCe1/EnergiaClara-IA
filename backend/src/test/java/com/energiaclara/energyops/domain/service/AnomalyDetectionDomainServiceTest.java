package com.energiaclara.energyops.domain.service;

import com.energiaclara.energyops.domain.AnomalySeverity;
import com.energiaclara.energyops.domain.AnomalyType;
import com.energiaclara.energyops.domain.event.DomainEvent;
import com.energiaclara.energyops.domain.exception.NoAnomalyDetectedException;
import com.energiaclara.energyops.domain.model.Anomaly;
import com.energiaclara.energyops.domain.vo.AnomalyScope;
import com.energiaclara.energyops.domain.vo.EnergyValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnomalyDetectionDomainServiceTest {

    @Test
    void consumptionWithinToleranceThrows() {
        AnomalyDetectionDomainService service = new AnomalyDetectionDomainService();

        assertThrows(NoAnomalyDetectedException.class, () -> service.detectAnomaly(
                AnomalyScope.of("FAC-1", "MTR-1"),
                AnomalyType.EXCESS_CONSUMPTION,
                EnergyValue.of(BigDecimal.valueOf(110)),
                EnergyValue.of(BigDecimal.valueOf(100)),
                BigDecimal.valueOf(15),
                BigDecimal.ONE,
                BigDecimal.ONE
        ));
    }

    @Test
    void consumptionOutsideToleranceCreatesDetectedAnomaly() {
        AnomalyDetectionDomainService service = new AnomalyDetectionDomainService();

        Anomaly anomaly = service.detectAnomaly(
                AnomalyScope.of("FAC-1", "MTR-1"),
                AnomalyType.EXCESS_CONSUMPTION,
                EnergyValue.of(BigDecimal.valueOf(180)),
                EnergyValue.of(BigDecimal.valueOf(100)),
                BigDecimal.valueOf(15),
                BigDecimal.ONE,
                BigDecimal.ONE
        );

        assertEquals(AnomalySeverity.HIGH, anomaly.getSeverity().level());
        assertEquals("energyops.anomaly.detected", anomaly.pullDomainEvents().stream()
                .map(DomainEvent::eventType)
                .findFirst()
                .orElse(""));
    }
}
