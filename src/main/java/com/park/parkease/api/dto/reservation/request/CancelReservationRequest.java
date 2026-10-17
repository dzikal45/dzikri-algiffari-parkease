package com.park.parkease.api.dto.reservation.request;

import com.park.parkease.api.constraint.SafeString;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelReservationRequest {
    @SafeString
    @NotBlank(message = "reason is required")
    private String reason;
}
