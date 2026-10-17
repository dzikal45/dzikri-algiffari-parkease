package com.park.parkease.service.parkingslot;

import com.park.parkease.domain.entities.ParkingLot;
import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.domain.enums.ReservationStatus;
import com.park.parkease.domain.enums.SlotStatus;
import com.park.parkease.domain.enums.VehicleType;
import com.park.parkease.exception.InvalidParameterException;
import com.park.parkease.repository.ParkingLotRepository;
import com.park.parkease.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingSlotServiceImpl implements ParkingSlotService {
    private final ParkingLotRepository parkingLotRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public List<ParkingSlot> findAvailableSlots(String lotId, VehicleType vehicleType, LocalDateTime startTime, LocalDateTime endTime) {
        return findAllSlot(lotId, vehicleType, startTime, endTime);
    }

    @Override
    public Optional<ParkingSlot> findAvailableSlot(String lotId, VehicleType vehicleType, LocalDateTime startTime, LocalDateTime endTime) {
        return findAllSlot(
                lotId, vehicleType,
                startTime, endTime).stream()
                .findFirst();
    }

    private List<ParkingSlot> findAllSlot(String lotId, VehicleType vehicleType,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        if (vehicleType == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return List.of();
        }
        if (startTime.toLocalDate().isBefore(LocalDate.now())) {
            throw new InvalidParameterException("startTime",
                    "startTime must be today or in the future");
        }

        ParkingLot parkingLot = parkingLotRepository.findById(lotId).orElse(null);
        if (parkingLot == null || parkingLot.getSlots() == null) {
            return List.of();
        }
        return parkingLot.getSlots().getOrDefault(vehicleType, java.util.List.of()).stream()
                .filter(slot -> slot.getStatus() == SlotStatus.AVAILABLE)
                .filter(slot -> hasNoOverlappingReservation(
                        lotId, slot.getSlotId(), startTime, endTime, null))
                .sorted(Comparator.comparingInt(ParkingSlot::getFloorLevel)
                        .thenComparing(ParkingSlot::getSlotId))
                .toList();

    }
    @Override
    public boolean hasNoOverlappingReservation(
            String lotId, String slotId, LocalDateTime startTime, LocalDateTime endTime,
            String excludedReservationId) {
        return reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING
                        || reservation.getStatus() == ReservationStatus.ACTIVE)
                .filter(reservation -> lotId.equals(reservation.getLotId())
                        && slotId.equals(reservation.getSlotId()))
                .filter(reservation -> !reservation.getReservationId().equals(excludedReservationId))
                .noneMatch(reservation -> overlaps(reservation, startTime, endTime));
    }

    private boolean overlaps(Reservation reservation, LocalDateTime startTime, LocalDateTime endTime) {
        return reservation.getPlannedStartTime() != null
                && reservation.getPlannedEndTime() != null
                && reservation.getPlannedStartTime().isBefore(endTime)
                && startTime.isBefore(reservation.getPlannedEndTime());
    }
}
