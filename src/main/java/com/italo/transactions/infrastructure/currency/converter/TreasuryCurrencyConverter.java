package com.italo.transactions.infrastructure.currency.converter;

import com.italo.transactions.domain.converter.CurrencyConverter;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.infrastructure.currency.client.TreasuryExchangeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TreasuryCurrencyConverter implements CurrencyConverter {

    private final TreasuryExchangeClient treasuryExchangeClient;
    private volatile List<String> currencies = List.of();

    @Override
    public Optional<CountryPurchaseTransaction> convertPurchaseToCountryPurchaseTransaction(String country, PurchaseTransaction purchaseTransaction) {
        Optional<String> countryDesc = getCurrencies().stream()
                .filter(it -> matchesCountry(it, country))
                .findFirst();
        return countryDesc.flatMap(s -> treasuryExchangeClient.findRate(s, purchaseTransaction.transactionDate())
                .map(it -> CountryPurchaseTransaction.create(purchaseTransaction, it.exchangeRate(), it.recordDate())));
    }

    List<String> getCurrencies() {
        if (currencies.isEmpty()) {
            synchronized (this) {
                if (currencies.isEmpty()) {
                    currencies = treasuryExchangeClient.getAllCurrencies();
                }
            }
        }

        return currencies;
    }

    public static boolean matchesCountry(String currencyDescription, String country) {
        String normalizedCountry = normalize(country);
        if (normalizedCountry.isEmpty()) {
            return false;
        }

        String normalizedDescription = normalize(currencyDescription);
        if (normalizedDescription.equals(normalizedCountry)) {
            return true;
        }

        int separatorIndex = currencyDescription.indexOf('-');
        if (separatorIndex < 0) {
            return false;
        }

        String countryName = currencyDescription.substring(0, separatorIndex);
        return normalize(countryName).equals(normalizedCountry);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
