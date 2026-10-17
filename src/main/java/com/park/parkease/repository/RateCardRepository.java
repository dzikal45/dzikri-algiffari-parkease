package com.park.parkease.repository;

import com.park.parkease.domain.entities.RateCard;
import com.park.parkease.domain.enums.VehicleType;

import java.util.Collection;
import java.util.Optional;

public interface RateCardRepository {
    Collection<RateCard> findAll();

    Optional<RateCard> findByVehicleType(VehicleType vehicleType);
}
