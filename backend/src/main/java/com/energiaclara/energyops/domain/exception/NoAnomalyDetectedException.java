package com.energiaclara.energyops.domain.exception;

import java.math.BigDecimal;

public class NoAnomalyDetectedException extends RuntimeException {
    public NoAnomalyDetectedException(String facilityId, String meterId, BigDecimal deviationPercent) {
        super("Sin anomalía en " + facilityId + "/" + meterId + " — desviación: " + deviationPercent + "%");
    }
}
