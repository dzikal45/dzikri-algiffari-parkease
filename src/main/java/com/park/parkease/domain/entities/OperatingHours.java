package com.park.parkease.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperatingHours {
    private Map<DayOfWeek, DailyHours> hoursByDay;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyHours {
        private LocalTime openTime;
        private LocalTime closeTime;
    }
}
