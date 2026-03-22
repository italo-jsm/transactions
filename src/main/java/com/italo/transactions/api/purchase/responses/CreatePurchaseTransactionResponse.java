package com.italo.transactions.api.purchase.responses;

import java.util.UUID;

public record CreatePurchaseTransactionResponse(UUID purchaseId) {

    public static CreatePurchaseTransactionResponse from(UUID purchaseId) {
        return new CreatePurchaseTransactionResponse(purchaseId);
    }
}
