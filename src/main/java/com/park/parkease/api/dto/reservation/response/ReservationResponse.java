package com.park.parkease.api.dto.reservation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationResponse {
    private String reservationId;
    private String slotId;
    private String lotId;
    private String status;
    private String plannedStartTime;
    private String plannedEndTime;
    private boolean lateCancellation;
}
