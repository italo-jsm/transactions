package com.italo.transactions.domain.converter;

import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;

import java.util.Optional;

public interface CurrencyConverter {
    Optional<CountryPurchaseTransaction> convertPurchaseToCountryPurchaseTransaction(String country, PurchaseTransaction purchaseTransaction);
}
