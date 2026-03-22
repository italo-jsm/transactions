package com.italo.transactions.domain.service;

import com.italo.transactions.domain.converter.CurrencyConverter;
import com.italo.transactions.domain.exception.EntityNotFoundException;
import com.italo.transactions.domain.exception.ExchangeRateNotFoundException;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseTransactionServiceTest {

    @Mock
    private PurchaseTransactionsRepository purchaseTransactionsRepository;

    @Mock
    private CurrencyConverter currencyConverter;

    @InjectMocks
    private PurchaseTransactionService purchaseTransactionService;

    @Captor
    private ArgumentCaptor<PurchaseTransaction> purchaseTransactionCaptor;

    @Test
    void shouldDelegatePurchaseTransactionPersistenceAndReturnGeneratedId() {
        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                UUID.randomUUID(),
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

    @Test
    void shouldReturnTransactionConvertedToCountryCurrencyWhenRateIsWithinSixMonths() {
        UUID transactionId = UUID.randomUUID();
        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                transactionId,
                "My product",
                LocalDate.of(2026, 3, 21),
                new BigDecimal("100.00")
        );
        CountryPurchaseTransaction convertedTransaction = CountryPurchaseTransaction.create(
                purchaseTransaction,
                new BigDecimal("5.25"),
                LocalDate.of(2026, 3, 1)
        );

        when(purchaseTransactionsRepository.findBydId(transactionId)).thenReturn(Optional.of(purchaseTransaction));
        when(currencyConverter.convertPurchaseToCountryPurchaseTransaction("Brazil", purchaseTransaction))
                .thenReturn(Optional.of(convertedTransaction));

        CountryPurchaseTransaction result = purchaseTransactionService.getTransactionInCountryCurrency(transactionId, "Brazil");

        assertThat(result).isEqualTo(convertedTransaction);
    }

    @Test
    void shouldThrowEntityNotFoundWhenPurchaseTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();

        when(purchaseTransactionsRepository.findBydId(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseTransactionService.getTransactionInCountryCurrency(transactionId, "Brazil"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Purchase transaction not found");
    }

    @Test
    void shouldThrowExchangeRateNotFoundWhenConverterDoesNotFindCountryRate() {
        UUID transactionId = UUID.randomUUID();
        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                transactionId,
                "My product",
                LocalDate.of(2026, 3, 21),
                new BigDecimal("100.00")
        );

        when(purchaseTransactionsRepository.findBydId(transactionId)).thenReturn(Optional.of(purchaseTransaction));
        when(currencyConverter.convertPurchaseToCountryPurchaseTransaction("Brazil", purchaseTransaction))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseTransactionService.getTransactionInCountryCurrency(transactionId, "Brazil"))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessage("Exchange rate not for the specified country");
    }

    @Test
    void shouldThrowExchangeRateNotFoundWhenRateIsNotWithinSixMonthsWindow() {
        UUID transactionId = UUID.randomUUID();
        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                transactionId,
                "My product",
                LocalDate.of(2026, 3, 21),
                new BigDecimal("100.00")
        );
        CountryPurchaseTransaction convertedTransaction = CountryPurchaseTransaction.create(
                purchaseTransaction,
                new BigDecimal("5.25"),
                LocalDate.of(2025, 9, 21)
        );

        when(purchaseTransactionsRepository.findBydId(transactionId)).thenReturn(Optional.of(purchaseTransaction));
        when(currencyConverter.convertPurchaseToCountryPurchaseTransaction("Brazil", purchaseTransaction))
                .thenReturn(Optional.of(convertedTransaction));

        assertThatThrownBy(() -> purchaseTransactionService.getTransactionInCountryCurrency(transactionId, "Brazil"))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessage("Exchange rate not found within 6 months for the specified country");
    }
}
