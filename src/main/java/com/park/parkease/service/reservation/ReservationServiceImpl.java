package com.park.parkease.service.reservation;

import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.domain.entities.ParkingLot;
import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.entities.RateCard;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.domain.enums.ReservationStatus;
import com.park.parkease.domain.enums.ResponseEnum;
import com.park.parkease.exception.ValidationException;
import com.park.parkease.repository.ParkingInvoiceRepository;
import com.park.parkease.repository.ParkingLotRepository;
import com.park.parkease.repository.RateCardRepository;
import com.park.parkease.repository.ReservationRepository;
import com.park.parkease.service.billing.BillingService;
import com.park.parkease.service.cache.CacheService;
import com.park.parkease.service.parkingslot.ParkingSlotService;
import com.park.parkease.service.reservation.model.CheckOutResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final RateCardRepository rateCardRepository;
    private final ParkingInvoiceRepository parkingInvoiceRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final BillingService billingService;
    private final CacheService<?, ?> cacheService;
    private final ParkingSlotService parkingSlotService;

    @Value("${parking.reservation.double-request-ttl:5m}")
    private Duration doubleRequestTtl;

    @Value("${parking.reservation.no-show-fee:5000.00}")
    private BigDecimal noShowFee;

    @Override
    public Reservation createReservation(Reservation reservation) {
        if (reservation == null || reservation.getPlannedStartTime() == null || reservation.getPlannedEndTime() == null
                || !reservation.getPlannedStartTime().isBefore(reservation.getPlannedEndTime())) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                    "plannedStartTime must be before plannedEndTime");
        }
        if (reservation.getPlannedStartTime().toLocalDate().isBefore(LocalDate.now())) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                    "plannedStartTime must be today or in the future");
        }
        rejectDuplicate("create:" + reservation.getCustomerId() + ':' + reservation.getLotId() + ':'
                + reservation.getVehicleType() + ':' + reservation.getPlannedStartTime() + ':' + reservation.getPlannedEndTime());

        return cacheService.lockRequest("reservation:" + reservation.getLotId(), () -> {
            validateEntryTime(reservation.getLotId(), reservation.getPlannedStartTime());
            ParkingSlot parkingSlot = parkingSlotService.findAvailableSlot(
                            reservation.getLotId(), reservation.getVehicleType(),
                            reservation.getPlannedStartTime(), reservation.getPlannedEndTime())
                    .orElseThrow(() -> new ValidationException(ResponseEnum.NOT_FOUND,
                            "No available parking slot"));

            reservation.setReservationId(UUID.randomUUID().toString());
            reservation.setSlotId(parkingSlot.getSlotId());
            reservation.setStatus(ReservationStatus.PENDING);

            reservationRepository.save(reservation);
            log.info("Reservation created: reservationId={}, lotId={}, slotId={}",
                    reservation.getReservationId(), reservation.getLotId(), reservation.getSlotId());
            return reservation;
        });
    }

    @Override
    public Reservation checkIn(String reservationId, LocalDateTime actualStartTime) {
        rejectDuplicate("check-in:" + reservationId + ':' + actualStartTime);

        return cacheService.lockRequest("reservation:" + reservationId, () -> {
            Reservation currentReservation = getReservation(reservationId);
            if (currentReservation.getStatus() != ReservationStatus.PENDING) {
                throw new ValidationException(ResponseEnum.CONFLICT, "Reservation cannot be checked in");
            }
            validateEntryTime(currentReservation.getLotId(), actualStartTime);
            LocalDateTime earliestCheckIn = currentReservation.getPlannedStartTime().minusMinutes(30);
            LocalDateTime latestCheckIn = currentReservation.getPlannedStartTime().plusMinutes(30);
            if (actualStartTime.isBefore(earliestCheckIn)) {
                throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                        "Reservation cannot be checked in before the requested time");
            }
            if (actualStartTime.isAfter(latestCheckIn)) {
                markNoShow(currentReservation);
                return currentReservation;
            }
            currentReservation.setActualStartTime(actualStartTime);
            currentReservation.setStatus(ReservationStatus.ACTIVE);
            reservationRepository.save(currentReservation);
            log.info("Reservation checked in: reservationId={}, status={}",
                    currentReservation.getReservationId(), currentReservation.getStatus());
            return currentReservation;
        });
    }

    @Override
    public CheckOutResult checkOut(String reservationId, LocalDateTime actualEndTime) {
        rejectDuplicate("check-out:" + reservationId + ':' + actualEndTime);

        Reservation reservation = getReservation(reservationId);
        return cacheService.lockRequest("reservation:" + reservation.getLotId(), () -> {
            Reservation currentReservation = getReservation(reservationId);
            if (currentReservation.getStatus() != ReservationStatus.ACTIVE
                    || !actualEndTime.isAfter(currentReservation.getActualStartTime())) {
                throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                        "Reservation cannot be checked out at the requested time");
            }

            currentReservation.setActualEndTime(actualEndTime);
            currentReservation.setStatus(ReservationStatus.COMPLETED);
            RateCard rateCard = rateCardRepository.findByVehicleType(currentReservation.getVehicleType())
                    .orElseThrow(() -> new ValidationException(ResponseEnum.NOT_FOUND, "Reservation cannot be checked out rate card vehicle not found"));
            ParkingInvoice invoice = billingService.generateInvoice(currentReservation, rateCard);
            parkingInvoiceRepository.save(invoice);
            reservationRepository.save(currentReservation);
            log.info("Reservation checked out: reservationId={}, invoiceId={}, totalAmount={}",
                    currentReservation.getReservationId(), invoice.getInvoiceId(), invoice.getTotalAmount());
            return new CheckOutResult(currentReservation, invoice);
        });
    }

    @Override
    public Reservation cancelReservation(String reservationId, String reason) {
        rejectDuplicate("cancel:" + reservationId + ':' + reason);
        return cacheService.lockRequest("reservation:" + reservationId, () -> {
            Reservation currentReservation = getReservation(reservationId);
            if (currentReservation.getStatus() != ReservationStatus.PENDING) {
                throw new ValidationException(ResponseEnum.CONFLICT, "Reservation cannot be cancelled");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cancellationDeadline = currentReservation.getPlannedStartTime().plusMinutes(30);
            if (now.isAfter(cancellationDeadline)) {
                markNoShow(currentReservation);
                throw new ValidationException(ResponseEnum.CONFLICT, "Reservation can no longer be cancelled");
            }
            currentReservation.setLateCancellation(
                    !now.isBefore(currentReservation.getPlannedStartTime().minusMinutes(30))
                            && !now.isAfter(cancellationDeadline));
            currentReservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(currentReservation);
            log.info("Reservation cancelled: reservationId={}, lateCancellation={}",
                    currentReservation.getReservationId(), currentReservation.isLateCancellation());
            return currentReservation;
        });
    }

    @Override
    public Reservation extendReservation(String reservationId, LocalDateTime newEndTime) {
        rejectDuplicate("extend:" + reservationId + ':' + newEndTime);

        Reservation reservation = getReservation(reservationId);
        return cacheService.lockRequest("reservation:" + reservation.getLotId(), () -> {
            Reservation currentReservation = getReservation(reservationId);
            if ((currentReservation.getStatus() != ReservationStatus.PENDING
                    && currentReservation.getStatus() != ReservationStatus.ACTIVE)
                    || !newEndTime.isAfter(currentReservation.getPlannedEndTime())) {
                throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                        "Reservation cannot be extended to the requested end time");
            }

            if (!parkingSlotService.hasNoOverlappingReservation(currentReservation.getLotId(), currentReservation.getSlotId(),
                    currentReservation.getPlannedStartTime(), newEndTime, currentReservation.getReservationId())) {
                throw new ValidationException(ResponseEnum.CONFLICT,
                        "Reservation extension overlaps another reservation");
            }

            currentReservation.setPlannedEndTime(newEndTime);
            reservationRepository.save(currentReservation);
            log.info("Reservation extended: reservationId={}, plannedEndTime={}",
                    currentReservation.getReservationId(), currentReservation.getPlannedEndTime());
            return currentReservation;
        });
    }

    @Override
    public ParkingInvoice findByReservationId(String reservationId) {
        return parkingInvoiceRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ValidationException(ResponseEnum.NOT_FOUND, "Invoice not found"));
    }

    @Override
    public void processNoShows(LocalDateTime now) {
        reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                .filter(reservation -> now.isAfter(reservation.getPlannedStartTime().plusMinutes(30)))
                .forEach(reservation -> cacheService.lockRequest("reservation:" + reservation.getReservationId(), () -> {
                    Reservation currentReservation = reservationRepository.findById(reservation.getReservationId()).orElse(null);
                    if (currentReservation != null && currentReservation.getStatus() == ReservationStatus.PENDING
                            && now.isAfter(currentReservation.getPlannedStartTime().plusMinutes(30))) {
                        markNoShow(currentReservation);
                    }
                    return null;
                }));
    }

    private Reservation getReservation(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER, "reservationId is required");
        }
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ValidationException(ResponseEnum.NOT_FOUND, "Reservation not found"));
    }

    private void rejectDuplicate(String requestKey) {
        if (cacheService.doubleRequest(requestKey, doubleRequestTtl)) {
            throw new ValidationException(ResponseEnum.CONFLICT, "Duplicate request");
        }
    }

    private void markNoShow(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            return;
        }
        if (parkingInvoiceRepository.findByReservationId(reservation.getReservationId()).isEmpty()) {
            parkingInvoiceRepository.save(billingService.generateNoShowInvoice(reservation, noShowFee));
        }
        reservation.setStatus(ReservationStatus.NO_SHOW);
        reservationRepository.save(reservation);
        log.info("Reservation marked no-show: reservationId={}, noShowFee={}",
                reservation.getReservationId(), noShowFee);
    }

    private void validateEntryTime(String lotId, LocalDateTime entryTime) {
        ParkingLot parkingLot = parkingLotRepository.findById(lotId)
                .orElseThrow(() -> new ValidationException(ResponseEnum.NOT_FOUND, "Parking lot not found"));
        if (parkingLot.getOperatingHours() == null || parkingLot.getOperatingHours().getHoursByDay() == null) {
            throw new ValidationException(ResponseEnum.CONFLICT, "Parking lot is closed");
        }

        var dailyHours = parkingLot.getOperatingHours().getHoursByDay().get(entryTime.getDayOfWeek());
        if (dailyHours == null || dailyHours.getOpenTime() == null || dailyHours.getCloseTime() == null) {
            throw new ValidationException(ResponseEnum.CONFLICT, "Parking lot is closed on " + entryTime.getDayOfWeek());
        }

        LocalDateTime openTime = entryTime.toLocalDate().atTime(dailyHours.getOpenTime());
        LocalDateTime closeTime = entryTime.toLocalDate().atTime(dailyHours.getCloseTime());
        if (entryTime.isBefore(openTime) || entryTime.isAfter(closeTime)) {
            throw new ValidationException(ResponseEnum.CONFLICT, "Parking lot is closed at the requested entry time");
        }
    }

}
