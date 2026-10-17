package com.park.parkease.api.dto.reservation.response;

import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.enums.SlotStatus;
import com.park.parkease.domain.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParkingSlotResponse {
    private List<ParkingSlotDto> parkingSlot;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParkingSlotDto {
        private String slotId;
        private String vehicleType;
        private String status;
        private int floorLevel;
    }
}
