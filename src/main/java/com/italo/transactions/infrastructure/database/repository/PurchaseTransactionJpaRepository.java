package com.italo.transactions.infrastructure.database.repository;

import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.domain.repository.PurchaseTransactionsRepository;
import com.italo.transactions.infrastructure.database.entity.PurchaseTransactionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseTransactionJpaRepository implements PurchaseTransactionsRepository {

    private final PurchaseTransactionJpaAdapter purchaseTransactionJpaAdapter;

    @Override
    public UUID savePurchaseTransaction(PurchaseTransaction purchaseTransaction) {
        return purchaseTransactionJpaAdapter.save(PurchaseTransactionEntity.fromDomain(purchaseTransaction)).getPurchaseId();
    }

    @Override
    public Optional<PurchaseTransaction> findById(UUID transactionId) {
        return purchaseTransactionJpaAdapter.findById(transactionId).map(PurchaseTransactionEntity::toDomain);
    }
}
