package com.park.parkease.exception;

import com.park.parkease.domain.enums.ResponseEnum;
import lombok.Getter;

@Getter
public class InvalidParameterException extends ValidationException {
    private final String field;

    public InvalidParameterException(String field, String reason) {
        super(ResponseEnum.INVALID_PARAMETER, reason);
        this.field = field;
    }
}
