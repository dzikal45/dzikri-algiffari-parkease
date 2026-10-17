package com.park.parkease.api.dto.error.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.park.parkease.api.dto.response.BaseResponse;
import com.park.parkease.domain.enums.ResponseEnum;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;


@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorBodyResponse {

    @JsonIgnore
    private ResponseEnum responseEnum;

    private ErrorListBodyResponse error;

    public ErrorBodyResponse(ResponseEnum responseEnum) {
        this.responseEnum = responseEnum;
        this.error = new ErrorListBodyResponse(new BaseResponse(responseEnum), null);
    }

    public ErrorBodyResponse(ResponseEnum responseEnum, String message) {
        this(responseEnum);
        this.error.setMessage(message);
    }

    public ErrorBodyResponse(ResponseEnum responseEnum, List<ValidationErrorResponse> errors) {
        this(responseEnum, null, errors);
    }

    public ErrorBodyResponse(ResponseEnum responseEnum, String message, List<ValidationErrorResponse> errors) {
        this.responseEnum = responseEnum;
        this.error = new ErrorListBodyResponse(new BaseResponse(responseEnum, message), errors);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    public static class ErrorListBodyResponse extends BaseResponse {

        private List<ValidationErrorResponse> errors;

        public ErrorListBodyResponse(BaseResponse baseError, List<ValidationErrorResponse> errors) {
            this.setCode(baseError.getCode());
            this.setMessage(baseError.getMessage());
            this.setErrors(errors);
        }
    }
}
