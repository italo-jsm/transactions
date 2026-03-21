package com.italo.transactions.infrastructure.database.repository;

import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.infrastructure.database.entity.PurchaseTransactionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(PurchaseTransactionJpaRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PurchaseTransactionJpaRepositoryTest {

    @Autowired
    private PurchaseTransactionJpaRepository purchaseTransactionJpaRepository;

    @Autowired
    private PurchaseTransactionJpaAdapter purchaseTransactionJpaAdapter;

    @Test
    void shouldPersistPurchaseTransactionAndReturnItsId() {
        PurchaseTransaction purchaseTransaction = new PurchaseTransaction(
                UUID.randomUUID(),
                "My product",
                LocalDate.of(2026, 3, 20),
                new BigDecimal("2499.99")
        );

        UUID savedId = purchaseTransactionJpaRepository.savePurchaseTransaction(purchaseTransaction);

        PurchaseTransactionEntity persistedEntity = purchaseTransactionJpaAdapter.findById(savedId).orElseThrow();

        assertThat(savedId).isEqualTo(purchaseTransaction.purchaseId());
        assertThat(persistedEntity.getPurchaseId()).isEqualTo(purchaseTransaction.purchaseId());
        assertThat(persistedEntity.getDescription()).isEqualTo(purchaseTransaction.description());
        assertThat(persistedEntity.getTransactionDate()).isEqualTo(purchaseTransaction.transactionDate());
        assertThat(persistedEntity.getPurchaseAmount()).isEqualByComparingTo(purchaseTransaction.purchaseAmount());
    }
}
