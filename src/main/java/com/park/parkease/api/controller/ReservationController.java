package com.park.parkease.api.controller;

import com.park.parkease.api.dto.reservation.request.*;
import com.park.parkease.api.dto.reservation.response.ReservationCheckOutResponse;
import com.park.parkease.api.dto.reservation.response.ReservationResponse;
import com.park.parkease.api.constant.ISOConstant;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.service.reservation.ReservationService;
import com.park.parkease.service.reservation.model.CheckOutResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping(value = "/reservations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toReservationResponse(reservationService.createReservation(toReservation(request))));
    }

    @PostMapping(value = "/reservations/{reservationId}/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservationResponse> checkIn(
            @PathVariable String reservationId, @Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(toReservationResponse(reservationService.checkIn(reservationId, request.getActualStartTime())));
    }

    @PostMapping(value = "/reservations/{reservationId}/check-out", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservationCheckOutResponse> checkOut(
            @PathVariable String reservationId, @Valid @RequestBody CheckOutRequest request) {
        return ResponseEntity.ok(toCheckOutResponse(reservationService.checkOut(
                reservationId, request.getActualEndTime())));
    }

    @DeleteMapping(value = "/reservations/{reservationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable String reservationId, @Valid @RequestBody CancelReservationRequest request) {
        return ResponseEntity.ok(toReservationResponse(reservationService.cancelReservation(reservationId, request.getReason())));
    }

    @PutMapping(value = "/reservations/{reservationId}/extend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservationResponse> extendReservation(
            @PathVariable String reservationId, @Valid @RequestBody ExtendReservationRequest request) {
        return ResponseEntity.ok(toReservationResponse(reservationService.extendReservation(reservationId, request.getNewEndTime())));
    }

    private Reservation toReservation(ReservationRequest request) {
        return Reservation.builder()
                .lotId(request.getLotId())
                .vehicleType(request.getVehicleType())
                .licensePlate(request.getLicensePlate())
                .customerId(request.getCustomerId())
                .plannedStartTime(request.getPlannedStartTime())
                .plannedEndTime(request.getPlannedEndTime())
                .build();
    }

    private ReservationResponse toReservationResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .slotId(reservation.getSlotId())
                .lotId(reservation.getLotId())
                .status(reservation.getStatus().name())
                .plannedStartTime(formatDateTime(reservation.getPlannedStartTime()))
                .plannedEndTime(formatDateTime(reservation.getPlannedEndTime()))
                .lateCancellation(reservation.isLateCancellation())
                .build();
    }

    private ReservationCheckOutResponse toCheckOutResponse(CheckOutResult result) {
        return ReservationCheckOutResponse.builder()
                .invoiceId(result.getInvoice().getInvoiceId())
                .customerId(result.getInvoice().getCustomerId())
                .totalAmount(String.valueOf(result.getInvoice().getTotalAmount()))
                .reservation(toReservationResponse(result.getReservation()))
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(ISOConstant.ISO_8601_FORMATTER);
    }
}
