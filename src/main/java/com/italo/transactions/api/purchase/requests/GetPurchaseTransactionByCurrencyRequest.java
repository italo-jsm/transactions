package com.italo.transactions.api.purchase.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetPurchaseTransactionByCurrencyRequest(

        @NotBlank(message = "{validation.purchase.currencyCode.required}")
        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "{validation.purchase.currencyCode.pattern}"
        )
        String currencyCode

) {}
