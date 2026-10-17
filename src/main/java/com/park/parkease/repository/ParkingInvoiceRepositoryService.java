package com.park.parkease.repository;

import com.park.parkease.domain.entities.ParkingInvoice;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParkingInvoiceRepositoryService implements ParkingInvoiceRepository {
    private final Map<String, ParkingInvoice> parkingInvoices = new ConcurrentHashMap<>();

    @Override
    public Optional<ParkingInvoice> findByReservationId(String reservationId) {
        return parkingInvoices.values().stream()
                .filter(invoice -> reservationId.equals(invoice.getReservationId()))
                .findFirst();
    }

    @Override
    public Collection<ParkingInvoice> findAll() {
        return parkingInvoices.values();
    }

    @Override
    public ParkingInvoice save(ParkingInvoice parkingInvoice) {
        parkingInvoices.put(parkingInvoice.getInvoiceId(), parkingInvoice);
        return parkingInvoice;
    }
}
