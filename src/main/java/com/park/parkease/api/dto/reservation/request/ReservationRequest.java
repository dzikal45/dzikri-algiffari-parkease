package com.park.parkease.api.dto.reservation.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.park.parkease.api.constant.ISOConstant;
import com.park.parkease.api.constraint.SafeString;
import com.park.parkease.domain.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationRequest {
    @NotNull(message = "invalid vehicleType")
    private VehicleType vehicleType;
    @SafeString
    @NotBlank
    private String licensePlate;
    @SafeString
    @NotBlank
    private String customerId;
    @SafeString
    @NotBlank
    private String lotId;
    @NotNull(message = "invalid format plannedStartTime")
    @DateTimeFormat(pattern = ISOConstant.ISO_8601)
    @JsonFormat(pattern = ISOConstant.ISO_8601)
    private LocalDateTime plannedStartTime;
    @NotNull(message = "invalid format plannedEndTime")
    @DateTimeFormat(pattern = ISOConstant.ISO_8601)
    @JsonFormat(pattern = ISOConstant.ISO_8601)
    private LocalDateTime plannedEndTime;

}
