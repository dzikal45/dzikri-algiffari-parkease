package com.park.parkease.service.billing;

import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.domain.entities.RateCard;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.domain.enums.ResponseEnum;
import com.park.parkease.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {
    private static final long HOUR_SECONDS = Duration.ofHours(1).getSeconds();
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    @Value("${parking.billing.hourly-rate-divisor:2}")
    private int hourlyRateDivisor = 2;

    @Override
    public ParkingInvoice generateInvoice(Reservation reservation, RateCard rateCard) {
        if (reservation == null || reservation.getReservationId() == null
                || reservation.getCustomerId() == null) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER, "Invalid reservation");
        }

        Charges charges = calculateCharges(
                reservation.getActualStartTime(), reservation.getActualEndTime(), rateCard);
        return ParkingInvoice.builder()
                .invoiceId(UUID.randomUUID().toString())
                .reservationId(reservation.getReservationId())
                .customerId(reservation.getCustomerId())
                .billedDurationMinutes(charges.billedDurationMinutes())
                .baseAmount(charges.baseAmount())
                .overnightSurchargeApplied(charges.overnightSurcharge())
                .discountAmount(ZERO)
                .totalAmount(money(charges.baseAmount().add(charges.overnightSurcharge())))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public ParkingInvoice generateNoShowInvoice(Reservation reservation, BigDecimal noShowFee) {
        if (reservation == null || reservation.getReservationId() == null || reservation.getCustomerId() == null
                || noShowFee == null || noShowFee.signum() < 0) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER, "Invalid no-show invoice input");
        }

        BigDecimal amount = money(noShowFee);
        return ParkingInvoice.builder()
                .invoiceId(UUID.randomUUID().toString())
                .reservationId(reservation.getReservationId())
                .customerId(reservation.getCustomerId())
                .billedDurationMinutes(0)
                .baseAmount(amount)
                .overnightSurchargeApplied(ZERO)
                .discountAmount(ZERO)
                .totalAmount(amount)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public BigDecimal calculateAmount(LocalDateTime actualStart, LocalDateTime actualEnd, RateCard rateCard) {
        Charges charges = calculateCharges(actualStart, actualEnd, rateCard);
        return money(charges.baseAmount().add(charges.overnightSurcharge()));
    }

    private Charges calculateCharges(LocalDateTime actualStart, LocalDateTime actualEnd, RateCard rateCard) {
        validateBillingInput(actualStart, actualEnd, rateCard);

        long totalSeconds = Duration.between(actualStart, actualEnd).getSeconds();
        long graceSeconds = Duration.ofMinutes(rateCard.getGracePeriodMinutes()).getSeconds();
        long billingBlockSeconds = billingBlockSeconds();
        if (totalSeconds <= graceSeconds) {
            return new Charges(0, ZERO, ZERO);
        }

        BigDecimal baseAmount = ZERO;
        LocalDateTime segmentStart = actualStart;
        while (segmentStart.isBefore(actualEnd)) {
            LocalDateTime nextMidnight = segmentStart.toLocalDate().plusDays(1).atStartOfDay();
            LocalDateTime segmentEnd = nextMidnight.isBefore(actualEnd) ? nextMidnight : actualEnd;
            long segmentSeconds = Duration.between(segmentStart, segmentEnd).getSeconds();
            long billableSeconds = Math.max(0, segmentSeconds - graceSeconds);
            graceSeconds = Math.max(0, graceSeconds - segmentSeconds);

            if (billableSeconds > 0) {
                long blocks = (billableSeconds + billingBlockSeconds - 1) / billingBlockSeconds;
                BigDecimal blockRate = money(rateCard.getHourlyRate().divide(
                        BigDecimal.valueOf(hourlyRateDivisor), 2, RoundingMode.HALF_UP));
                BigDecimal dailyAmount = money(blockRate.multiply(BigDecimal.valueOf(blocks)));
                baseAmount = money(baseAmount.add(dailyAmount.min(rateCard.getDailyCap())));
            }
            segmentStart = segmentEnd;
        }

        long midnightCrossings = ChronoUnit.DAYS.between(
                actualStart.toLocalDate(), actualEnd.toLocalDate());
        BigDecimal overnightSurcharge = money(rateCard.getOvernightSurcharge()
                .multiply(BigDecimal.valueOf(midnightCrossings)));
        long billedDurationMinutes = Math.max(0,
                Duration.between(actualStart, actualEnd).toMinutes() - rateCard.getGracePeriodMinutes());
        return new Charges(billedDurationMinutes, baseAmount, overnightSurcharge);
    }

    private void validateBillingInput(LocalDateTime actualStart, LocalDateTime actualEnd, RateCard rateCard) {
        if (actualStart == null || actualEnd == null || !actualStart.isBefore(actualEnd)
                || rateCard == null
                || rateCard.getHourlyRate() == null || rateCard.getHourlyRate().signum() < 0
                || rateCard.getDailyCap() == null || rateCard.getDailyCap().signum() < 0
                || rateCard.getOvernightSurcharge() == null || rateCard.getOvernightSurcharge().signum() < 0
                || rateCard.getGracePeriodMinutes() < 0) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER, "Invalid billing input");
        }
    }

    private long billingBlockSeconds() {
        if (hourlyRateDivisor <= 0 || HOUR_SECONDS % hourlyRateDivisor != 0) {
            throw new ValidationException(ResponseEnum.INVALID_PARAMETER,
                    "parking.billing.hourly-rate-divisor must divide one hour evenly");
        }
        return HOUR_SECONDS / hourlyRateDivisor;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private record Charges(long billedDurationMinutes, BigDecimal baseAmount, BigDecimal overnightSurcharge) {
    }
}
