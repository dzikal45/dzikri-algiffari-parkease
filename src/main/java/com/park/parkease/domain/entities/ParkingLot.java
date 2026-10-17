package com.park.parkease.domain.entities;

import com.park.parkease.domain.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParkingLot {
    private String lotId;
    private String name;
    private String location;
    private Map<VehicleType, List<ParkingSlot>> slots;
    private OperatingHours operatingHours;
}
