package com.italo.transactions.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseTransaction(
        UUID purchaseId,
        String description,
        LocalDate transactionDate,
        BigDecimal purchaseAmount
) {

    public static PurchaseTransaction create(
            String description,
            LocalDate transactionDate,
            BigDecimal purchaseAmount
    ) {
        return new PurchaseTransaction(
                UUID.randomUUID(),
                description,
                transactionDate,
                purchaseAmount
        );
    }
}
