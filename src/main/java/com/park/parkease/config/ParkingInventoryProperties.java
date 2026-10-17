package com.park.parkease.config;

import com.park.parkease.domain.enums.VehicleType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "parking.inventory")
public class ParkingInventoryProperties {
    private List<Lot> lots = new ArrayList<>();

    @Data
    public static class Lot {
        private String lotId;
        private String name;
        private String location;
        private int floorCount;
        private Map<VehicleType, Integer> slotsPerFloor = new EnumMap<>(VehicleType.class);
        private Map<DayOfWeek, DailyHours> operatingHours = new EnumMap<>(DayOfWeek.class);
    }

    @Data
    public static class DailyHours {
        private LocalTime openTime;
        private LocalTime closeTime;
    }
}
