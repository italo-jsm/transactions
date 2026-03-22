package com.italo.transactions.infrastructure.currency.client;

import java.net.http.HttpConnectTimeoutException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.italo.transactions.domain.exception.ExchangeRateNotFoundException;
import com.italo.transactions.domain.exception.ExternalDependencyException;
import com.italo.transactions.domain.exception.ExternalDependencyTimeoutException;
import com.italo.transactions.infrastructure.currency.client.dto.ExchangeRateResponse;
import com.italo.transactions.infrastructure.currency.client.dto.TreasuryApiResponse;
import com.italo.transactions.infrastructure.currency.client.dto.TreasuryRateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

@Component
public class TreasuryExchangeClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);
    private static final int CURRENCY_PAGE_SIZE = 500;

    private final RestClient restClient;
    private final String path;
    private final Duration timeout;

    @Autowired
    public TreasuryExchangeClient(
            @Value("${treasury.api.base-url}") String baseUrl,
            @Value("${treasury.api.path}") String path,
            @Value("${treasury.api.timeout:3s}") Duration timeout) {

        this(baseUrl, path, timeout, true);
    }

    TreasuryExchangeClient(String baseUrl, String path) {
        this(baseUrl, path, DEFAULT_TIMEOUT, false);
    }

    TreasuryExchangeClient(String baseUrl, String path, Duration timeout, boolean ignored) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        this.path = path;
        this.timeout = timeout;
    }


    public Optional<ExchangeRateResponse> findRate(String countryCurrencyDesc, LocalDate purchaseDate) {
        String filter = String.format(
                "country_currency_desc:in:(%s),record_date:lte:%s",
                countryCurrencyDesc,
                purchaseDate
        );

        TreasuryApiResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("fields", "country_currency_desc,exchange_rate,record_date,effective_date")
                            .queryParam("filter", filter)
                            .queryParam("sort", "-record_date")
                            .queryParam("page[size]", "1")
                            .build())
                    .retrieve()
                    .body(TreasuryApiResponse.class);
        } catch (ResourceAccessException exception) {
            throw mapTimeoutException(exception);
        } catch (RestClientException exception) {
            throw mapDependencyException(exception);
        }

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
        List<String> currencies = new ArrayList<>();

        for (int pageNumber = 1; ; pageNumber++) {
            TreasuryApiResponse response = fetchCurrenciesPage(pageNumber);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }

            response.data().stream()
                    .map(TreasuryRateDto::countryCurrencyDesc)
                    .forEach(currencies::add);

            if (response.data().size() < CURRENCY_PAGE_SIZE) {
                break;
            }
        }

        return currencies.stream()
                .distinct()
                .toList();
    }

    private TreasuryApiResponse fetchCurrenciesPage(int pageNumber) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("fields", "country_currency_desc")
                            .queryParam("page[size]", String.valueOf(CURRENCY_PAGE_SIZE))
                            .queryParam("page[number]", String.valueOf(pageNumber))
                            .build())
                    .retrieve()
                    .body(TreasuryApiResponse.class);
        } catch (ResourceAccessException exception) {
            throw mapTimeoutException(exception);
        } catch (RestClientException exception) {
            throw mapDependencyException(exception);
        }
    }

    private RuntimeException mapTimeoutException(ResourceAccessException exception) {
        if (isTimeout(exception)) {
            return new ExternalDependencyTimeoutException("Treasury API did not respond within " + timeout.toSeconds() + " seconds", exception);
        }

        return new ExternalDependencyException("Treasury API is unavailable", exception);
    }

    private ExternalDependencyException mapDependencyException(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return new ExternalDependencyException(
                    "Treasury API responded with status " + responseException.getStatusCode().value(),
                    exception
            );
        }

        return new ExternalDependencyException("Treasury API is unavailable", exception);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof TimeoutException
                    || current instanceof HttpConnectTimeoutException) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

}
