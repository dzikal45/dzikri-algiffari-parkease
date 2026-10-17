package com.park.parkease.repository;

import com.park.parkease.domain.entities.ParkingInvoice;

import java.util.Collection;
import java.util.Optional;

public interface ParkingInvoiceRepository {
    Optional<ParkingInvoice> findByReservationId(String reservationId);

    Collection<ParkingInvoice> findAll();

    ParkingInvoice save(ParkingInvoice parkingInvoice);
}
