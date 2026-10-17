package com.park.parkease.api.dto.reservation.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.park.parkease.api.constant.ISOConstant;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {
    @NotNull(message = "actualStartTime is required")
    @DateTimeFormat(pattern = ISOConstant.ISO_8601)
    @JsonFormat(pattern = ISOConstant.ISO_8601)
    private LocalDateTime actualStartTime;
}
