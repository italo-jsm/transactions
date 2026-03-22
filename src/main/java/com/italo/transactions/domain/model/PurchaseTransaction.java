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
            UUID uuid,
            String description,
            LocalDate transactionDate,
            BigDecimal purchaseAmount
    ) {
        return new PurchaseTransaction(
                uuid,
                description,
                transactionDate,
                purchaseAmount
        );
    }
}
