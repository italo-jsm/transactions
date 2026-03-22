package com.italo.transactions.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

public record CountryPurchaseTransaction(
        UUID purchaseId,
        String description,
        LocalDate transactionDate,
        BigDecimal purchaseAmount,
        BigDecimal convertedPurchasedAmount,
        BigDecimal exchangeRate,
        LocalDate recordDate
){

    public static CountryPurchaseTransaction create(
            PurchaseTransaction purchaseTransaction,
            BigDecimal exchangeRate,
            LocalDate recordDate
    ) {
        return new CountryPurchaseTransaction(
                purchaseTransaction.purchaseId(),
                purchaseTransaction.description(),
                purchaseTransaction.transactionDate(),
                purchaseTransaction.purchaseAmount(),
                purchaseTransaction.purchaseAmount().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP),
                exchangeRate,
                recordDate
        );
    }
}
