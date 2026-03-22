package com.italo.transactions.infrastructure.currency.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.italo.transactions.domain.exception.ExchangeRateNotFoundException;
import com.italo.transactions.infrastructure.currency.client.dto.ExchangeRateResponse;
import com.italo.transactions.infrastructure.currency.client.dto.TreasuryApiResponse;
import com.italo.transactions.infrastructure.currency.client.dto.TreasuryRateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TreasuryExchangeClient {

    private final RestClient restClient;
    private final String path;

    public TreasuryExchangeClient(
            @Value("${treasury.api.base-url}") String baseUrl,
            @Value("${treasury.api.path}") String path) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.path = path;
    }


    public Optional<ExchangeRateResponse> findRate(String countryCurrencyDesc, LocalDate purchaseDate) {
        String filter = String.format(
                "country_currency_desc:in:(%s),record_date:lte:%s",
                countryCurrencyDesc,
                purchaseDate
        );

        TreasuryApiResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("fields", "country_currency_desc,exchange_rate,record_date,effective_date")
                        .queryParam("filter", filter)
                        .queryParam("sort", "-record_date")
                        .queryParam("page[size]", "1")
                        .build())
                .retrieve()
                .body(TreasuryApiResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new ExchangeRateNotFoundException("No exchange rate found for " + countryCurrencyDesc);
        }

        TreasuryRateDto treasuryRate = response.data().getFirst();

        return Optional.of(new ExchangeRateResponse(
                treasuryRate.countryCurrencyDesc(),
                new BigDecimal(treasuryRate.exchangeRate()),
                treasuryRate.recordDate()
        ));
    }

    public List<String> getAllCurrencies() {

        TreasuryApiResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("fields", "country_currency_desc")
                        .queryParam("page[size]", "500")
                        .build())
                .retrieve()
                .body(TreasuryApiResponse.class);

        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data().stream()
                .map(TreasuryRateDto::countryCurrencyDesc)
                .distinct()
                .toList();
    }

}