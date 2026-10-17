package com.park.parkease.repository;

import com.park.parkease.domain.entities.ParkingSlot;

import java.util.Optional;

public interface ParkingSlotRepository {
    ParkingSlot save(ParkingSlot parkingSlot);
    Optional<ParkingSlot> findById(String parkingSlotId);
}
