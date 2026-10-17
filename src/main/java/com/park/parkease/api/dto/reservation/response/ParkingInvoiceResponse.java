package com.park.parkease.api.dto.reservation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingInvoiceResponse {
    private String invoiceId;
    private String reservationId;
    private String customerId;
    private long billedDurationMinutes;
    private BigDecimal baseAmount;
    private BigDecimal overnightSurchargeApplied;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String generatedAt;
}
