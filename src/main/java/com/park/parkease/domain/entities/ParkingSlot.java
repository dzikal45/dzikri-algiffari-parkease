package com.park.parkease.domain.entities;

import com.park.parkease.domain.enums.SlotStatus;
import com.park.parkease.domain.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParkingSlot {
    private String slotId;
    private VehicleType vehicleType;
    private SlotStatus status;
    private int floorLevel;
}
