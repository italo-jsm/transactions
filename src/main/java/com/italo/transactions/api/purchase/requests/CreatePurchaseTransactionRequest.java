package com.italo.transactions.api.purchase.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePurchaseTransactionRequest(

        @NotBlank(message = "{validation.purchase.description.required}")
        @Size(max = 50, message = "{validation.purchase.description.size}")
        String description,

        @NotBlank(message = "{validation.purchase.transactionDate.required}")
        @JsonFormat(pattern = "yyyy-MM-dd")
        String transactionDate,

        @NotNull(message = "{validation.purchase.amount.required}")
        @DecimalMin(value = "0.01", inclusive = true, message = "{validation.purchase.amount.min}")
        @Digits(integer = 12, fraction = 10, message = "{validation.purchase.amount.digits}")
        BigDecimal amount

) {}
