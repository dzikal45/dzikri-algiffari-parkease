package com.park.parkease.domain.entities;

import com.park.parkease.domain.enums.ReservationStatus;
import com.park.parkease.domain.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {
    private String reservationId;
    private String lotId;
    private String slotId;
    private VehicleType vehicleType;
    private String licensePlate;
    private String customerId;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private ReservationStatus status;
    private boolean lateCancellation;
}
