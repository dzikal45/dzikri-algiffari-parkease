package com.park.parkease.exception.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.park.parkease.api.dto.error.response.ErrorBodyResponse;
import com.park.parkease.api.dto.error.response.ValidationErrorResponse;
import com.park.parkease.api.util.MapperErrorUtil;
import com.park.parkease.domain.enums.ResponseEnum;
import com.park.parkease.exception.InvalidParameterException;
import com.park.parkease.exception.ValidationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class ValidationExceptionHandler {
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorBodyResponse> handleInvalidParameterException(InvalidParameterException e) {
        return invalidParameter(e.getField(), e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorBodyResponse> handleValidationException(ValidationException e) {
        log.warn("ValidationException: {}", e.getMessage());
        return ResponseEntity.status(e.getResponseEnum().getHttpStatus()).body(new ErrorBodyResponse(e.getResponseEnum(), e.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorBodyResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorBodyResponse(ResponseEnum.INVALID_PARAMETER,
                        MapperErrorUtil.toErrorResponses(e.getConstraintViolations())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBodyResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(new ErrorBodyResponse(ResponseEnum.INVALID_PARAMETER,
                e.getBindingResult().getFieldErrors().stream()
                        .map(error -> new ValidationErrorResponse(error.getField(), error.getDefaultMessage()))
                        .toList()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorBodyResponse> handleMissingRequestParameter(MissingServletRequestParameterException e) {
        return invalidParameter(e.getParameterName(), "is required");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorBodyResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return invalidParameter(e.getName(), "has an invalid value");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBodyResponse> handleUnreadableMessage(HttpMessageNotReadableException e) {
        return invalidParameter(invalidField(e), "has an invalid value");
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorBodyResponse> handleUnknownException(Exception ex) {
        log.warn("Handle Unknown Exception: {} ", ex.getMessage());
        var response = new ErrorBodyResponse(ResponseEnum.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private ResponseEntity<ErrorBodyResponse> invalidParameter(String field, String reason) {
        return ResponseEntity.badRequest().body(new ErrorBodyResponse(ResponseEnum.INVALID_PARAMETER,
                List.of(new ValidationErrorResponse(field, reason))));
    }

    private String invalidField(HttpMessageNotReadableException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof InvalidFormatException invalidFormatException) {
                return invalidFormatException.getPath().stream()
                        .map(reference -> reference.getFieldName())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("request");
            }
            cause = cause.getCause();
        }
        return "request";
    }
}
