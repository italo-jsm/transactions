package com.italo.transactions.infrastructure.database.repository;

import com.italo.transactions.infrastructure.database.entity.PurchaseTransactionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PurchaseTransactionJpaAdapterTest {

    @Autowired
    private PurchaseTransactionJpaAdapter purchaseTransactionJpaAdapter;

    @Test
    void shouldSaveAndRetrieveEntityUsingInMemoryDatabase() {
        PurchaseTransactionEntity entity = new PurchaseTransactionEntity(
                UUID.randomUUID(),
                "my product",
                LocalDate.of(2026, 3, 19),
                new BigDecimal("799.90")
        );

        PurchaseTransactionEntity savedEntity = purchaseTransactionJpaAdapter.save(entity);

        PurchaseTransactionEntity foundEntity = purchaseTransactionJpaAdapter.findById(savedEntity.getPurchaseId()).orElseThrow();

        assertThat(foundEntity.getPurchaseId()).isEqualTo(savedEntity.getPurchaseId());
        assertThat(foundEntity.getDescription()).isEqualTo(entity.getDescription());
        assertThat(foundEntity.getTransactionDate()).isEqualTo(entity.getTransactionDate());
        assertThat(foundEntity.getPurchaseAmount()).isEqualByComparingTo(entity.getPurchaseAmount());
    }
}
