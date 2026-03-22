package com.italo.transactions.infrastructure.database.entity;

import com.italo.transactions.domain.model.PurchaseTransaction;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "purchase_transactions")
public class PurchaseTransactionEntity {

    @Id
    @Column(name = "purchase_id", nullable = false)
    private UUID purchaseId;

    @Column(name = "description", nullable = false, length = 50)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "purchase_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal purchaseAmount;

    public PurchaseTransactionEntity() {
    }

    public PurchaseTransactionEntity(UUID purchaseId, String description, LocalDate transactionDate, BigDecimal purchaseAmount) {
        this.purchaseId = purchaseId;
        this.description = description;
        this.transactionDate = transactionDate;
        this.purchaseAmount = purchaseAmount;
    }

    public static PurchaseTransactionEntity fromDomain(PurchaseTransaction purchaseTransaction){
        return new PurchaseTransactionEntity(
                purchaseTransaction.purchaseId(),
                purchaseTransaction.description(),
                purchaseTransaction.transactionDate(),
                purchaseTransaction.purchaseAmount()
        );
    }

    public PurchaseTransaction toDomain() {
        return PurchaseTransaction.create(
                this.purchaseId,
                this.description,
                this.transactionDate,
                this.purchaseAmount
        );
    }
}
