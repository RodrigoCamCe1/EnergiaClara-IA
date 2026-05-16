package com.energiaclara.energyops.domain.vo;

import java.math.BigDecimal;

public record EnergyValue(BigDecimal kwh) {
    public EnergyValue {
        if (kwh == null || kwh.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("kwh debe ser mayor o igual a cero");
        }
    }

    public static EnergyValue of(BigDecimal kwh) {
        return new EnergyValue(kwh);
    }

    public EnergyValue excessOver(EnergyValue baseline) {
        return new EnergyValue(this.kwh.subtract(baseline.kwh).max(BigDecimal.ZERO));
    }

    public EnergyValue add(EnergyValue other) {
        return new EnergyValue(this.kwh.add(other.kwh));
    }
}
