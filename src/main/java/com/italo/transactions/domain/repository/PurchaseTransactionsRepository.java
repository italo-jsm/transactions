package com.italo.transactions.domain.repository;

import com.italo.transactions.domain.model.PurchaseTransaction;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseTransactionsRepository {

    UUID savePurchaseTransaction(PurchaseTransaction purchaseTransaction);
    Optional<PurchaseTransaction> findBydId(UUID transactionId);
}
