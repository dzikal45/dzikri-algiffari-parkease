package com.park.parkease.repository;

import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.enums.ResponseEnum;
import com.park.parkease.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ParkingSlotRepositoryService implements ParkingSlotRepository {
    private final Map<String, ParkingSlot> parkingSlots = new ConcurrentHashMap<>();

    @Override
    public ParkingSlot save(ParkingSlot parkingSlot) {
        if (parkingSlot == null || StringUtils.isBlank(parkingSlot.getSlotId())) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                    "slotId must not be null");
        }
        ParkingSlot existing = parkingSlots.putIfAbsent(parkingSlot.getSlotId(), parkingSlot);
        if (existing != null) {
            throw new ValidationException(ResponseEnum.CONFLICT,
                    "Parking lot already exists: " + parkingSlot.getSlotId());
        }
        return parkingSlot;
    }

    @Override
    public Optional<ParkingSlot> findById(String parkingSlotId) {
        if (parkingSlotId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parkingSlots.get(parkingSlotId));
    }
}
