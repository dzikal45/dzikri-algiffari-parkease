package com.park.parkease.api.controller;

import com.park.parkease.api.constant.ISOConstant;
import com.park.parkease.api.dto.reservation.response.ParkingSlotResponse;
import com.park.parkease.domain.enums.VehicleType;
import com.park.parkease.service.parkingslot.ParkingSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/lots", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ParkingLotController {
    private final ParkingSlotService parkingSlotService;

    @GetMapping("/{lotId}/availability")
    public ResponseEntity<ParkingSlotResponse> findAvailableSlots(
            @PathVariable String lotId,
            @RequestParam VehicleType vehicleType,
            @RequestParam @DateTimeFormat(pattern = ISOConstant.ISO_8601) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = ISOConstant.ISO_8601) LocalDateTime endTime) {
        return ResponseEntity.ok(ParkingSlotResponse.builder()
                .parkingSlot(parkingSlotService.findAvailableSlots(lotId, vehicleType, startTime, endTime).stream()
                        .map(slot -> ParkingSlotResponse.ParkingSlotDto.builder()
                                .slotId(slot.getSlotId())
                                .vehicleType(slot.getVehicleType().name())
                                .status(slot.getStatus().name())
                                .floorLevel(slot.getFloorLevel())
                                .build())
                        .toList())
                .build());
    }
}
