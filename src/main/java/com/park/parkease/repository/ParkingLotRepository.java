package com.park.parkease.repository;

import com.park.parkease.domain.entities.ParkingLot;
import java.util.Optional;

public interface ParkingLotRepository {
    Optional<ParkingLot> findById(String lotId);

    ParkingLot save(ParkingLot parkingLot);
}
