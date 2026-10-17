package com.park.parkease.api.controller;

import com.park.parkease.exception.handler.ValidationExceptionHandler;
import com.park.parkease.exception.InvalidParameterException;
import com.park.parkease.service.parkingslot.ParkingSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParkingLotControllerTest {
    private MockMvc mockMvc;
    private ParkingSlotService parkingSlotService;

    @BeforeEach
    void setUp() {
        parkingSlotService = mock(ParkingSlotService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ParkingLotController(parkingSlotService))
                .setControllerAdvice(new ValidationExceptionHandler())
                .build();
    }

    @Test
    void findAvailableSlots_withMissingQueryParameter_returnsStructuredBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/lots/LOT-JKT01/availability")
                        .param("vehicleType", "CAR")
                        .param("startTime", "2026-10-19T09:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_parameter"))
                .andExpect(jsonPath("$.error.errors[0].field").value("endTime"))
                .andExpect(jsonPath("$.error.errors[0].reason").value("is required"));
    }

    @Test
    void findAvailableSlots_withInvalidQueryParameter_returnsStructuredBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/lots/LOT-JKT01/availability")
                        .param("vehicleType", "BOAT")
                        .param("startTime", "2026-10-19T09:00:00")
                        .param("endTime", "2026-10-19T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_parameter"))
                .andExpect(jsonPath("$.error.errors[0].field").value("vehicleType"))
                .andExpect(jsonPath("$.error.errors[0].reason").value("has an invalid value"));
    }

    @Test
    void findAvailableSlots_withPastStartTime_returnsStructuredBadRequest() throws Exception {
        when(parkingSlotService.findAvailableSlots(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InvalidParameterException("startTime", "startTime must be today or in the future"));

        mockMvc.perform(get("/api/v1/lots/LOT-JKT01/availability")
                        .param("vehicleType", "CAR")
                        .param("startTime", "2020-01-01T09:00:00")
                        .param("endTime", "2020-01-01T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_parameter"))
                .andExpect(jsonPath("$.error.errors[0].field").value("startTime"))
                .andExpect(jsonPath("$.error.errors[0].reason")
                        .value("startTime must be today or in the future"));
    }
}
