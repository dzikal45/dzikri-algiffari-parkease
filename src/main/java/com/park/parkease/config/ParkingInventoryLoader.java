package com.park.parkease.config;

import com.park.parkease.domain.entities.OperatingHours;
import com.park.parkease.domain.entities.ParkingLot;
import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.enums.SlotStatus;
import com.park.parkease.domain.enums.VehicleType;
import com.park.parkease.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParkingInventoryLoader implements ApplicationRunner {
    private final ParkingInventoryProperties inventoryProperties;
    private final ParkingLotRepository parkingLotRepository;

    @Override
    public void run(ApplicationArguments args) {
        inventoryProperties.getLots().forEach(this::loadLot);
    }

    private void loadLot(ParkingInventoryProperties.Lot lot) {
        log.info("Seeding data parking lot by properties ...");
        if (lot.getFloorCount() <= 0 || lot.getSlotsPerFloor() == null || lot.getSlotsPerFloor().isEmpty()
                || lot.getSlotsPerFloor().values().stream().anyMatch(count -> count == null || count <= 0)) {
            throw new IllegalStateException("Parking inventory requires positive floor and slot counts");
        }

        Map<VehicleType, List<ParkingSlot>> slotsByVehicle = new EnumMap<>(VehicleType.class);
        lot.getSlotsPerFloor().forEach((vehicleType, slotsPerFloor) -> {
            List<ParkingSlot> slots = new ArrayList<>();
            for (int floor = 1; floor <= lot.getFloorCount(); floor++) {
                for (int sequence = 1; sequence <= slotsPerFloor; sequence++) {
                    slots.add(ParkingSlot.builder()
                            .slotId(lot.getLotId() + "-" + vehicleType + "-F" + floor + "-" + String.format("%03d", sequence))
                            .vehicleType(vehicleType)
                            .status(SlotStatus.AVAILABLE)
                            .floorLevel(floor)
                            .build());
                }
            }
            slotsByVehicle.put(vehicleType, slots);
        });

        parkingLotRepository.save(ParkingLot.builder()
                .lotId(lot.getLotId())
                .name(lot.getName())
                .location(lot.getLocation())
                .slots(slotsByVehicle)
                .operatingHours(toOperatingHours(lot.getOperatingHours()))
                .build());
        log.info("Finish seeding data parking lot by properties ...");
    }

    private OperatingHours toOperatingHours(Map<DayOfWeek, ParkingInventoryProperties.DailyHours> configuredHours) {
        Map<DayOfWeek, OperatingHours.DailyHours> hoursByDay = new EnumMap<>(DayOfWeek.class);
        if (configuredHours != null) {
            configuredHours.forEach((day, hours) -> {
                if (hours == null || hours.getOpenTime() == null || hours.getCloseTime() == null
                        || !hours.getOpenTime().isBefore(hours.getCloseTime())) {
                    throw new IllegalStateException("Operating hours must have an opening time before closing time");
                }
                hoursByDay.put(day, OperatingHours.DailyHours.builder()
                        .openTime(hours.getOpenTime())
                        .closeTime(hours.getCloseTime())
                        .build());
            });
        }
        return OperatingHours.builder().hoursByDay(hoursByDay).build();
    }
}
