package com.italo.transactions.domain.service;

import com.italo.transactions.domain.converter.CurrencyConverter;
import com.italo.transactions.domain.exception.EntityNotFoundException;
import com.italo.transactions.domain.exception.ExchangeRateNotFoundException;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.domain.repository.PurchaseTransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseTransactionService {

    private final PurchaseTransactionsRepository purchaseTransactionsRepository;
    private final CurrencyConverter currencyConverter;

    public UUID create(PurchaseTransaction purchaseTransaction){
        return purchaseTransactionsRepository.savePurchaseTransaction(purchaseTransaction);
    }

    public CountryPurchaseTransaction getTransactionInCountryCurrency(UUID transactionId, String country){
        PurchaseTransaction purchaseTransaction = purchaseTransactionsRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase transaction not found"));
        CountryPurchaseTransaction countryPurchaseTransaction = currencyConverter
                .convertPurchaseToCountryPurchaseTransaction(country, purchaseTransaction)
                .orElseThrow(() -> new ExchangeRateNotFoundException("Exchange rate not for the specified country"));

        if (isWithinAllowedConversionWindow(countryPurchaseTransaction)) {
            return countryPurchaseTransaction;
        }

        throw new ExchangeRateNotFoundException("Exchange rate not found within 6 months for the specified country");
    }

    private boolean isWithinAllowedConversionWindow(CountryPurchaseTransaction transaction) {
        LocalDate limitDate = transaction.transactionDate().minusMonths(6);
        return !transaction.recordDate().isBefore(limitDate);
    }
}
