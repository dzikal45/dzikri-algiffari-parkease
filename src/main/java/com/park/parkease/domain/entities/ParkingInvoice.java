package com.park.parkease.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParkingInvoice {
    private String invoiceId;
    private String reservationId;
    private String customerId;
    private long billedDurationMinutes;
    private BigDecimal baseAmount;
    private BigDecimal overnightSurchargeApplied;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private LocalDateTime generatedAt;
}
