package com.italo.transactions.infrastructure.database.repository;

import com.italo.transactions.infrastructure.database.entity.PurchaseTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PurchaseTransactionJpaAdapter extends JpaRepository<PurchaseTransactionEntity, UUID> {
}
