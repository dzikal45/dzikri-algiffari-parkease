package com.park.parkease.api.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SafeStringValidator implements ConstraintValidator<SafeString, String> {
    private static final Logger log = LoggerFactory.getLogger(SafeStringValidator.class);
    private SafeString safeString;

    public void initialize(SafeString safeString) {
        this.safeString = safeString;
    }

    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        String regex = this.safeString.regexp();
        return Objects.toString(value, "").matches(regex);
    }
}
