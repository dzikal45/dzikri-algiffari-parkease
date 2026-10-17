package com.park.parkease.service.billing;

import com.park.parkease.domain.entities.RateCard;
import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.domain.enums.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BillingServiceImplTest {
    private final BillingService service = new BillingServiceImpl();

    @Test
    void calculateAmount_withStandardHourlyStay_returnsExpectedCharge() {
        BigDecimal amount = service.calculateAmount(
                LocalDateTime.of(2024, 3, 15, 9, 0),
                LocalDateTime.of(2024, 3, 15, 12, 45),
                rateCard(VehicleType.CAR, "5000", "50000", "20000", 15));

        assertThat(amount).isEqualByComparingTo("17500.00");
    }

    @Test
    void calculateAmount_withMultiDayStay_appliesDailyCapAndOvernightSurcharge() {
        BigDecimal amount = service.calculateAmount(
                LocalDateTime.of(2024, 3, 15, 7, 0),
                LocalDateTime.of(2024, 3, 16, 9, 0),
                rateCard(VehicleType.TRUCK, "15000", "120000", "30000", 15));

        assertThat(amount).isEqualByComparingTo("270000.00");
    }

    @Test
    void calculateAmount_withStayInsideGracePeriod_returnsZero() {
        BigDecimal amount = service.calculateAmount(
                LocalDateTime.of(2024, 3, 15, 14, 0),
                LocalDateTime.of(2024, 3, 15, 14, 12),
                rateCard(VehicleType.MOTORCYCLE, "3000", "50000", "0", 15));

        assertThat(amount).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateAmount_onThirtyMinuteBoundary_doesNotRoundUp() {
        BigDecimal amount = service.calculateAmount(
                LocalDateTime.of(2024, 3, 15, 9, 0),
                LocalDateTime.of(2024, 3, 15, 9, 30),
                rateCard(VehicleType.CAR, "5000", "50000", "0", 0));

        assertThat(amount).isEqualByComparingTo("2500.00");
    }

    @Test
    void calculateAmount_acrossTwoMidnights_appliesSurchargeTwice() {
        BigDecimal amount = service.calculateAmount(
                LocalDateTime.of(2024, 3, 15, 10, 0),
                LocalDateTime.of(2024, 3, 17, 10, 0),
                rateCard(VehicleType.CAR, "0", "0", "20000", 0));

        assertThat(amount).isEqualByComparingTo("40000.00");
    }

    @Test
    void calculateAmount_withConfiguredHourlyRateDivisor_usesMatchingBillingBlocks() {
        BillingServiceImpl configuredService = new BillingServiceImpl();
        ReflectionTestUtils.setField(configuredService, "hourlyRateDivisor", 4);

        BigDecimal amount = configuredService.calculateAmount(
                LocalDateTime.of(2024, 3, 15, 9, 0),
                LocalDateTime.of(2024, 3, 15, 9, 16),
                rateCard(VehicleType.CAR, "6000", "50000", "0", 0));

        assertThat(amount).isEqualByComparingTo("3000.00");
    }

    @Test
    void generateNoShowInvoice_chargesConfiguredFeeWithoutParkingDuration() {
        ParkingInvoice invoice = service.generateNoShowInvoice(Reservation.builder()
                .reservationId("reservation-1")
                .customerId("customer-1")
                .build(), new BigDecimal("5000"));

        assertThat(invoice.getBilledDurationMinutes()).isZero();
        assertThat(invoice.getBaseAmount()).isEqualByComparingTo("5000.00");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("5000.00");
        assertThat(invoice.getOvernightSurchargeApplied()).isEqualByComparingTo("0.00");
    }

    private RateCard rateCard(
            VehicleType vehicleType, String hourlyRate, String dailyCap, String overnightSurcharge, int gracePeriodMinutes) {
        return RateCard.builder()
                .vehicleType(vehicleType)
                .hourlyRate(new BigDecimal(hourlyRate))
                .dailyCap(new BigDecimal(dailyCap))
                .overnightSurcharge(new BigDecimal(overnightSurcharge))
                .gracePeriodMinutes(gracePeriodMinutes)
                .build();
    }
}
