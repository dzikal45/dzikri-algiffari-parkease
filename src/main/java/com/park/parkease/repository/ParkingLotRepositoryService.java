package com.park.parkease.repository;

import com.park.parkease.domain.entities.ParkingLot;
import com.park.parkease.domain.enums.ResponseEnum;
import com.park.parkease.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class ParkingLotRepositoryService implements ParkingLotRepository {
    private static final Pattern LOT_ID_PATTERN = Pattern.compile("LOT-[A-Z0-9]{3,8}");
    private final Map<String, ParkingLot> parkingLots = new ConcurrentHashMap<>();

    @Override
    public Optional<ParkingLot> findById(String lotId) {
        if (lotId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parkingLots.get(lotId));
    }

    @Override
    public ParkingLot save(ParkingLot parkingLot) {
        if (parkingLot == null || parkingLot.getLotId() == null
                || !LOT_ID_PATTERN.matcher(parkingLot.getLotId()).matches()) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                    "lotId must match LOT-[A-Z0-9]{3,8}");
        }

        ParkingLot existing = parkingLots.putIfAbsent(parkingLot.getLotId(), parkingLot);
        if (existing != null) {
            throw new ValidationException(ResponseEnum.CONFLICT,
                    "Parking lot already exists: " + parkingLot.getLotId());
        }

        return parkingLot;
    }
}
