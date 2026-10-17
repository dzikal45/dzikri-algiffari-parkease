package com.park.parkease.api.constraint;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {SafeStringValidator.class}
)
public @interface SafeString {
    String message() default "Invalid Safe String Value";

    String regexp() default "^[a-zA-Z0-9.\\-/+,=_:'@% ]*$";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
