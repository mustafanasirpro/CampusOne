package com.campusone.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class Utf8ByteLengthValidator
        implements ConstraintValidator<Utf8ByteLength, CharSequence> {

    private int maximumBytes;

    @Override
    public void initialize(Utf8ByteLength constraintAnnotation) {
        maximumBytes = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(
            CharSequence value,
            ConstraintValidatorContext context) {
        return value == null
                || value.toString().getBytes(StandardCharsets.UTF_8).length <= maximumBytes;
    }
}
