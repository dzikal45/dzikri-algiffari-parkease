package com.park.parkease.api.controller;

import com.park.parkease.api.dto.reservation.response.ParkingInvoiceResponse;
import com.park.parkease.api.constant.ISOConstant;
import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ParkingInvoiceController {
    private final ReservationService reservationService;

    @GetMapping("/{reservationId}")
    public ResponseEntity<ParkingInvoiceResponse> findInvoice(@PathVariable String reservationId) {
        ParkingInvoice invoice = reservationService.findByReservationId(reservationId);
        return ResponseEntity.ok(ParkingInvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .reservationId(invoice.getReservationId())
                .customerId(invoice.getCustomerId())
                .billedDurationMinutes(invoice.getBilledDurationMinutes())
                .baseAmount(invoice.getBaseAmount())
                .overnightSurchargeApplied(invoice.getOvernightSurchargeApplied())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .generatedAt(formatDateTime(invoice.getGeneratedAt()))
                .build());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(ISOConstant.ISO_8601_FORMATTER);
    }
}
