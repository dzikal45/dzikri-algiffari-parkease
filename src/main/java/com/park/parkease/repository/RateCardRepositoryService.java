package com.park.parkease.repository;

import com.park.parkease.domain.entities.RateCard;
import com.park.parkease.domain.enums.VehicleType;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateCardRepositoryService implements RateCardRepository {
    private final Map<VehicleType, RateCard> rateCards = new ConcurrentHashMap<>();

    @Override
    public Collection<RateCard> findAll() {
        return rateCards.values();
    }

    @Override
    public Optional<RateCard> findByVehicleType(VehicleType vehicleType) {
        return Optional.ofNullable(rateCards.get(vehicleType));
    }
}
