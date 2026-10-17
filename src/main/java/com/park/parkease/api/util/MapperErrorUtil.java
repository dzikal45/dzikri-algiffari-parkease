package com.park.parkease.api.util;

import com.park.parkease.api.dto.error.response.ValidationErrorResponse;
import jakarta.validation.ConstraintViolation;

import java.util.List;
import java.util.Set;

public final class MapperErrorUtil {
    private MapperErrorUtil() {
    }

    public static List<ValidationErrorResponse> toErrorResponses(Set<ConstraintViolation<?>> constraintViolations) {
        return constraintViolations.stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    return new ValidationErrorResponse(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
                })
                .toList();
    }
}
