package com.italo.transactions.infrastructure.currency.converter;

import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.infrastructure.currency.client.TreasuryExchangeClient;
import com.italo.transactions.infrastructure.currency.client.dto.ExchangeRateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreasuryCurrencyConverterTest {

    @Mock
    private TreasuryExchangeClient treasuryExchangeClient;

    @InjectMocks
    private TreasuryCurrencyConverter treasuryCurrencyConverter;

    @Test
    void shouldInitializeAvailableCurrenciesFromTreasuryApi() {
        when(treasuryExchangeClient.getAllCurrencies()).thenReturn(List.of("Brazil-Real", "Canada-Dollar"));

        treasuryCurrencyConverter.init();

        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                UUID.randomUUID(),
                "Notebook",
                LocalDate.of(2026, 3, 20),
                new BigDecimal("100.00")
        );
        ExchangeRateResponse exchangeRateResponse = new ExchangeRateResponse(
                "Brazil-Real",
                new BigDecimal("5.25"),
                LocalDate.of(2026, 3, 19)
        );

        when(treasuryExchangeClient.findRate("Brazil-Real", purchaseTransaction.transactionDate()))
                .thenReturn(Optional.of(exchangeRateResponse));

        Optional<CountryPurchaseTransaction> convertedTransaction = treasuryCurrencyConverter
                .convertPurchaseToCountryPurchaseTransaction("brazil", purchaseTransaction);

        assertThat(convertedTransaction).isPresent();
        assertThat(convertedTransaction.orElseThrow().convertedPurchasedAmount()).isEqualByComparingTo("525.00");
        verify(treasuryExchangeClient).findRate("Brazil-Real", purchaseTransaction.transactionDate());
    }

    @Test
    void shouldReturnEmptyWhenCountryIsNotPresentInAvailableCurrencies() {
        when(treasuryExchangeClient.getAllCurrencies()).thenReturn(List.of("Canada-Dollar"));

        treasuryCurrencyConverter.init();

        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                UUID.randomUUID(),
                "Notebook",
                LocalDate.of(2026, 3, 20),
                new BigDecimal("100.00")
        );

        Optional<CountryPurchaseTransaction> convertedTransaction = treasuryCurrencyConverter
                .convertPurchaseToCountryPurchaseTransaction("Brazil", purchaseTransaction);

        assertThat(convertedTransaction).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenTreasuryClientDoesNotReturnRate() {
        when(treasuryExchangeClient.getAllCurrencies()).thenReturn(List.of("Brazil-Real"));

        treasuryCurrencyConverter.init();

        PurchaseTransaction purchaseTransaction = PurchaseTransaction.create(
                UUID.randomUUID(),
                "Notebook",
                LocalDate.of(2026, 3, 20),
                new BigDecimal("100.00")
        );

        when(treasuryExchangeClient.findRate("Brazil-Real", purchaseTransaction.transactionDate()))
                .thenReturn(Optional.empty());

        Optional<CountryPurchaseTransaction> convertedTransaction = treasuryCurrencyConverter
                .convertPurchaseToCountryPurchaseTransaction("Brazil", purchaseTransaction);

        assertThat(convertedTransaction).isEmpty();
    }

    @Test
    void shouldMatchCountryIgnoringCase() {
        assertThat(TreasuryCurrencyConverter.containsIgnoreCase("Brazil-Real", "BRAZIL")).isTrue();
        assertThat(TreasuryCurrencyConverter.containsIgnoreCase("Brazil-Real", "canada")).isFalse();
    }
}
