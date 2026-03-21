package com.italo.transactions.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldValidationError> fieldErrors
) {

    public record FieldValidationError(
            String field,
            String message
    ) {}
}
