package com.park.parkease.api.controller;

import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.domain.enums.ReservationStatus;
import com.park.parkease.exception.handler.ValidationExceptionHandler;
import com.park.parkease.service.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest {
    private MockMvc mockMvc;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservationController(reservationService))
                .setControllerAdvice(new ValidationExceptionHandler())
                .build();
    }

    @Test
    void createReservation_withInvalidDateTime_returnsFieldValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lotId": "LOT-JKT01",
                                  "vehicleType": "CAR",
                                  "licensePlate": "B1234XYZ",
                                  "customerId": "customer-1",
                                  "plannedStartTime": "not-a-date",
                                  "plannedEndTime": "2026-10-19T10:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_parameter"))
                .andExpect(jsonPath("$.error.errors[0].field").value("plannedStartTime"))
                .andExpect(jsonPath("$.error.errors[0].reason").value("has an invalid value"));
    }

    @Test
    void cancelReservation_returnsLateCancellationFlag() throws Exception {
        when(reservationService.cancelReservation("reservation-1", "change"))
                .thenReturn(Reservation.builder()
                        .reservationId("reservation-1")
                        .slotId("CAR-F1-001")
                        .lotId("LOT-JKT01")
                        .status(ReservationStatus.CANCELLED)
                        .lateCancellation(true)
                        .build());

        mockMvc.perform(delete("/api/v1/reservations/reservation-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"change\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.lateCancellation").value(true));
    }
}
