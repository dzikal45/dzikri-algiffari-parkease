package com.park.parkease.service.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReservationNoShowScheduler {
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${parking.reservation.no-show-check-interval:60000}")
    public void processNoShows() {
        reservationService.processNoShows(LocalDateTime.now());
    }
}
