package com.park.parkease.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseEnum {
    SUCCESS("success", "success",HttpStatus.OK),
    SUCCESS_CREATED( "success_created",
            "Request has been fulfilled, but result on another resource", HttpStatus.CREATED),
    INTERNAL_SERVER_ERROR("internal_server_error",  "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_PARAMETER("invalid_parameter", "Invalid parameter", HttpStatus.BAD_REQUEST),
    CONFLICT("conflict","Duplicate Request with same request id", HttpStatus.CONFLICT),
    NOT_FOUND("not_found", "Request or data not found", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
