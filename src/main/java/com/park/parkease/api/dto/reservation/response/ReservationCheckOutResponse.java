package com.park.parkease.api.dto.reservation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationCheckOutResponse {
    private ReservationResponse reservation;
    private String invoiceId;
    private String customerId;
    private String totalAmount;
}
