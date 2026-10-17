package com.park.parkease.exception;

import com.park.parkease.domain.enums.ResponseEnum;
import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
    private final ResponseEnum responseEnum;
    private final String message;

    public ValidationException(ResponseEnum responseEnum) {
        super(responseEnum.getMessage());
        this.responseEnum = responseEnum;
        this.message = responseEnum.getMessage();
    }

    public ValidationException(ResponseEnum responseEnum, String customMessage) {
        super(customMessage);
        this.responseEnum = responseEnum;
        this.message = customMessage;
    }
}
