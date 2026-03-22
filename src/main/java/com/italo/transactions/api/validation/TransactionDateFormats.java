package com.italo.transactions.api.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

public final class TransactionDateFormats {

    private static final DateTimeFormatter STRICT_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private TransactionDateFormats() {
    }

    public static Optional<LocalDate> parse(String value) {
        try {
            return Optional.of(LocalDate.parse(value, STRICT_DATE_FORMATTER));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }
}
