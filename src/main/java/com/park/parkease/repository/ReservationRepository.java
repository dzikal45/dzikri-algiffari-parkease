package com.park.parkease.repository;

import com.park.parkease.domain.entities.Reservation;

import java.util.Collection;
import java.util.Optional;

public interface ReservationRepository {
    Optional<Reservation> findById(String reservationId);

    Collection<Reservation> findAll();

    Reservation save(Reservation reservation);
}
