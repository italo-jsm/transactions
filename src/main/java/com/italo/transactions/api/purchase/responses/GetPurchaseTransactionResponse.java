package com.italo.transactions.api.purchase.responses;

import com.italo.transactions.domain.model.CountryPurchaseTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GetPurchaseTransactionResponse(
        UUID purchaseId,
        String description,
        LocalDate transactionDate,
        BigDecimal purchaseAmount,
        BigDecimal exchangeRate,
        BigDecimal convertedPurchasedAmount
) {
    public static GetPurchaseTransactionResponse create(
            CountryPurchaseTransaction countryPurchaseTransaction
    ) {
        return new GetPurchaseTransactionResponse(
                countryPurchaseTransaction.purchaseId(),
                countryPurchaseTransaction.description(),
                countryPurchaseTransaction.transactionDate(),
                countryPurchaseTransaction.purchaseAmount(),
                countryPurchaseTransaction.exchangeRate(),
                countryPurchaseTransaction.convertedPurchasedAmount()
        );
    }
}
