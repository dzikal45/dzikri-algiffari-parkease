package com.park.parkease.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.park.parkease.domain.enums.ResponseEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse {

    private String code;
    private String message;

    public BaseResponse(ResponseEnum responseEnum) {
        this.setCode(responseEnum.getCode());
        this.setMessage(responseEnum.getMessage());
    }

    public BaseResponse(ResponseEnum responseEnum, String message) {
        this(responseEnum);
        this.setMessage(message);
    }

    public BaseResponse(String code, String type, String message) {
        this.code = code;
        this.message = message;
    }
}
