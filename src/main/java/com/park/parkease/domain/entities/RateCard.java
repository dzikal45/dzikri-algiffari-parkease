package com.park.parkease.domain.entities;

import com.park.parkease.domain.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RateCard {
    private VehicleType vehicleType;
    private BigDecimal hourlyRate;
    private BigDecimal dailyCap;
    private BigDecimal overnightSurcharge;

    @Builder.Default
    private int gracePeriodMinutes = 15;
}
