package com.italo.transactions.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionDateValidator implements ConstraintValidator<ValidTransactionDate, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return TransactionDateFormats.parse(value).isPresent();
    }
}
