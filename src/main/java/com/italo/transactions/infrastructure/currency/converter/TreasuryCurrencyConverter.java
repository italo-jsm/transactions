package com.italo.transactions.infrastructure.currency.converter;

import com.italo.transactions.domain.converter.CurrencyConverter;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.infrastructure.currency.client.TreasuryExchangeClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TreasuryCurrencyConverter implements CurrencyConverter {

    private final TreasuryExchangeClient treasuryExchangeClient;
    private List<String> currencies;

    @PostConstruct
    public void init() {
        this.currencies = treasuryExchangeClient.getAllCurrencies();
    }

    @Override
    public Optional<CountryPurchaseTransaction> convertPurchaseToCountryPurchaseTransaction(String country, PurchaseTransaction purchaseTransaction) {
        Optional<String> countryDesc = currencies.stream().filter(it -> containsIgnoreCase(it, country)).findFirst();
        return countryDesc.flatMap(s -> treasuryExchangeClient.findRate(s, purchaseTransaction.transactionDate())
                .map(it -> CountryPurchaseTransaction.create(purchaseTransaction, it.exchangeRate(), it.recordDate())));
    }

    public static boolean containsIgnoreCase(String str, String search) {
        return str.toLowerCase().contains(search.toLowerCase());
    }
}
