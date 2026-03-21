package com.italo.transactions.domain.service;

import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.domain.repository.PurchaseTransactionsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseTransactionServiceTest {

    @Mock
    private PurchaseTransactionsRepository purchaseTransactionsRepository;

    @InjectMocks
    private PurchaseTransactionService purchaseTransactionService;

    @Captor
    private ArgumentCaptor<PurchaseTransaction> purchaseTransactionCaptor;

    @Test
    void shouldDelegatePurchaseTransactionPersistenceAndReturnGeneratedId() {
        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                "My product",
                LocalDate.of(2026, 3, 21),
                new BigDecimal("5999.90")
        );
        UUID expectedId = UUID.randomUUID();

        when(purchaseTransactionsRepository.savePurchaseTransaction(purchaseTransaction)).thenReturn(expectedId);

        UUID createdId = purchaseTransactionService.create(purchaseTransaction);

        verify(purchaseTransactionsRepository).savePurchaseTransaction(purchaseTransactionCaptor.capture());
        assertThat(purchaseTransactionCaptor.getValue()).isEqualTo(purchaseTransaction);
        assertThat(createdId).isEqualTo(expectedId);
    }
}
