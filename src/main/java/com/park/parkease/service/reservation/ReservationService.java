package com.park.parkease.service.reservation;

import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.service.reservation.model.CheckOutResult;

import java.time.LocalDateTime;

public interface ReservationService {
    Reservation createReservation(Reservation reservation);
    Reservation checkIn(String reservationId, LocalDateTime actualStartTime);
    CheckOutResult checkOut(String reservationId, LocalDateTime actualEndTime);
    Reservation cancelReservation(String reservationId, String reason);
    Reservation extendReservation(String reservationId, LocalDateTime newEndTime);
    ParkingInvoice findByReservationId(String reservationId);
    void processNoShows(LocalDateTime now);

}
