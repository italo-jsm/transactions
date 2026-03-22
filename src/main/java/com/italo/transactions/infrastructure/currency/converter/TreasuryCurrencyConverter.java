package com.italo.transactions.infrastructure.currency.converter;

import com.italo.transactions.domain.converter.CurrencyConverter;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.infrastructure.currency.client.TreasuryExchangeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class TreasuryCurrencyConverter implements CurrencyConverter {

    private final TreasuryExchangeClient treasuryExchangeClient;
    private final Duration cacheTtl;
    private final Clock clock;
    private volatile CurrencyCache currencyCache = CurrencyCache.empty();

    @Autowired
    public TreasuryCurrencyConverter(
            TreasuryExchangeClient treasuryExchangeClient,
            @Value("${treasury.api.currencies-cache-ttl:6h}") Duration cacheTtl
    ) {
        this(treasuryExchangeClient, cacheTtl, Clock.systemUTC());
    }

    TreasuryCurrencyConverter(TreasuryExchangeClient treasuryExchangeClient, Duration cacheTtl, Clock clock) {
        this.treasuryExchangeClient = treasuryExchangeClient;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    @Override
    public Optional<CountryPurchaseTransaction> convertPurchaseToCountryPurchaseTransaction(String country, PurchaseTransaction purchaseTransaction) {
        Optional<String> countryDesc = getCurrencies().stream()
                .filter(it -> matchesCountry(it, country))
                .findFirst();
        return countryDesc.flatMap(s -> treasuryExchangeClient.findRate(s, purchaseTransaction.transactionDate())
                .map(it -> CountryPurchaseTransaction.create(purchaseTransaction, it.exchangeRate(), it.recordDate())));
    }

    List<String> getCurrencies() {
        Instant now = clock.instant();
        if (currencyCache.isExpiredAt(now)) {
            synchronized (this) {
                if (currencyCache.isExpiredAt(now)) {
                    currencyCache = CurrencyCache.of(
                            treasuryExchangeClient.getAllCurrencies(),
                            now.plus(cacheTtl)
                    );
                }
            }
        }

        return currencyCache.currencies();
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

    private record CurrencyCache(List<String> currencies, Instant expiresAt) {
        private static CurrencyCache empty() {
            return new CurrencyCache(List.of(), Instant.EPOCH);
        }

        private static CurrencyCache of(List<String> currencies, Instant expiresAt) {
            return new CurrencyCache(List.copyOf(currencies), expiresAt);
        }

        private boolean isExpiredAt(Instant instant) {
            return currencies.isEmpty() || !instant.isBefore(expiresAt);
        }
    }
}
