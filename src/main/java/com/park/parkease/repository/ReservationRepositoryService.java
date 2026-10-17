package com.park.parkease.repository;

import com.park.parkease.domain.entities.Reservation;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReservationRepositoryService implements ReservationRepository {
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public Collection<Reservation> findAll() {
        return reservations.values();
    }

    @Override
    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getReservationId(), reservation);
        return reservation;
    }
}
