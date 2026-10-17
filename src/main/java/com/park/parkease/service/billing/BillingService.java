package com.park.parkease.service.billing;

import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.domain.entities.RateCard;
import com.park.parkease.domain.entities.Reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface BillingService {
    ParkingInvoice generateInvoice(
            Reservation reservation,
            RateCard rateCard
    );
    ParkingInvoice generateNoShowInvoice(Reservation reservation, BigDecimal noShowFee);
    BigDecimal calculateAmount(
            LocalDateTime actualStart,
            LocalDateTime actualEnd,
            RateCard rateCard
    );
}
