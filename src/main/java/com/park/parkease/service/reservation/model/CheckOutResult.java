package com.park.parkease.service.reservation.model;

import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.domain.entities.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckOutResult {
    private Reservation reservation;
    private ParkingInvoice invoice;
}
