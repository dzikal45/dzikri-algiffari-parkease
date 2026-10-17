package com.park.parkease.service.parkingslot;

import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingSlotService {
    List<ParkingSlot> findAvailableSlots(
            String lotId,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Optional<ParkingSlot> findAvailableSlot(
            String lotId,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    boolean hasNoOverlappingReservation(
            String lotId,
            String slotId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String excludedReservationId
    );
}
