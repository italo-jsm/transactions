package com.italo.transactions.infrastructure.currency.client;

import com.italo.transactions.domain.exception.ExchangeRateNotFoundException;
import com.italo.transactions.domain.exception.ExternalDependencyException;
import com.italo.transactions.domain.exception.ExternalDependencyTimeoutException;
import com.italo.transactions.infrastructure.currency.client.dto.ExchangeRateResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
class TreasuryExchangeClientTest {

    private static final String PATH = "/v1/accounting/od/rates_of_exchange";
    private static final int MOCK_SERVER_PORT = 1080;

    @Container
    static final GenericContainer<?> MOCK_SERVER = new GenericContainer<>("mockserver/mockserver:5.15.0")
            .withExposedPorts(MOCK_SERVER_PORT);

    @Test
    void shouldReturnMostRecentRateForCountryBeforePurchaseDate() {
        MockServerClient mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT));
        mockServerClient.reset();
        mockServerClient
                .when(request()
                        .withPath(PATH)
                        .withQueryStringParameters(
                                Parameter.param("fields", "country_currency_desc,exchange_rate,record_date,effective_date"),
                                Parameter.param("filter", "country_currency_desc:in:(Brazil-Real),record_date:lte:2026-03-20"),
                                Parameter.param("sort", "-record_date"),
                                Parameter.param("page[size]", "1")
                        ))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "country_currency_desc": "Brazil-Real",
                                      "exchange_rate": "5.4321",
                                      "record_date": "2026-03-19",
                                      "effective_date": "2026-03-20"
                                    }
                                  ]
                                }
                                """));

        TreasuryExchangeClient client = new TreasuryExchangeClient(
                "http://" + MOCK_SERVER.getHost() + ":" + MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT),
                PATH
        );

        ExchangeRateResponse response = client.findRate("Brazil-Real", LocalDate.of(2026, 3, 20)).orElseThrow();

        assertThat(response.currencyDescription()).isEqualTo("Brazil-Real");
        assertThat(response.exchangeRate()).isEqualByComparingTo("5.4321");
        assertThat(response.recordDate()).isEqualTo(LocalDate.of(2026, 3, 19));
    }

    @Test
    void shouldThrowWhenTreasuryApiDoesNotReturnRates() {
        MockServerClient mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT));
        mockServerClient.reset();
        mockServerClient
                .when(request()
                        .withPath(PATH)
                        .withQueryStringParameters(
                                Parameter.param("fields", "country_currency_desc,exchange_rate,record_date,effective_date"),
                                Parameter.param("filter", "country_currency_desc:in:(Brazil-Real),record_date:lte:2026-03-20"),
                                Parameter.param("sort", "-record_date"),
                                Parameter.param("page[size]", "1")
                        ))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                                {
                                  "data": []
                                }
                                """));

        TreasuryExchangeClient client = new TreasuryExchangeClient(
                "http://" + MOCK_SERVER.getHost() + ":" + MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT),
                PATH
        );

        assertThatThrownBy(() -> client.findRate("Brazil-Real", LocalDate.of(2026, 3, 20)))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessage("No exchange rate found for Brazil-Real");
    }

    @Test
    void shouldReturnDistinctCurrenciesFromTreasuryApi() {
        MockServerClient mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT));
        mockServerClient.reset();
        mockServerClient
                .when(request()
                        .withPath(PATH)
                        .withQueryStringParameters(
                                Parameter.param("fields", "country_currency_desc"),
                                Parameter.param("page[size]", "500")
                        ))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                                {
                                  "data": [
                                    {"country_currency_desc": "Brazil-Real"},
                                    {"country_currency_desc": "Canada-Dollar"},
                                    {"country_currency_desc": "Brazil-Real"}
                                  ]
                                }
                                """));

        TreasuryExchangeClient client = new TreasuryExchangeClient(
                "http://" + MOCK_SERVER.getHost() + ":" + MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT),
                PATH
        );

        List<String> currencies = client.getAllCurrencies();

        assertThat(currencies).containsExactly("Brazil-Real", "Canada-Dollar");
    }

    @Test
    void shouldReturnEmptyListWhenTreasuryApiResponseBodyIsEmpty() {
        MockServerClient mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT));
        mockServerClient.reset();
        mockServerClient
                .when(request()
                        .withPath(PATH)
                        .withQueryStringParameters(
                                Parameter.param("fields", "country_currency_desc"),
                                Parameter.param("page[size]", "500")
                        ))
                .respond(response()
                        .withStatusCode(200));

        TreasuryExchangeClient client = new TreasuryExchangeClient(
                "http://" + MOCK_SERVER.getHost() + ":" + MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT),
                PATH
        );

        assertThat(client.getAllCurrencies()).isEmpty();
    }

    @Test
    void shouldThrowTimeoutExceptionWhenTreasuryTakesTooLongToRespond() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext(PATH, exchange -> {
                try {
                    Thread.sleep(2_000);
                    byte[] responseBody = """
                            {
                              "data": [
                                {
                                  "country_currency_desc": "Brazil-Real",
                                  "exchange_rate": "5.4321",
                                  "record_date": "2026-03-19",
                                  "effective_date": "2026-03-20"
                                }
                              ]
                            }
                            """.getBytes();
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseBody.length);
                    try (OutputStream outputStream = exchange.getResponseBody()) {
                        outputStream.write(responseBody);
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            server.start();

            TreasuryExchangeClient client = new TreasuryExchangeClient(
                    "http://localhost:" + server.getAddress().getPort(),
                    PATH,
                    Duration.ofSeconds(1),
                    false
            );

            assertThatThrownBy(() -> client.findRate("Brazil-Real", LocalDate.of(2026, 3, 20)))
                    .isInstanceOf(ExternalDependencyTimeoutException.class)
                    .hasMessage("Treasury API did not respond within 1 seconds");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    @Test
    void shouldThrowExternalDependencyExceptionWhenTreasuryRespondsWithServerError() {
        MockServerClient mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT));
        mockServerClient.reset();
        mockServerClient
                .when(request()
                        .withPath(PATH)
                        .withQueryStringParameters(
                                Parameter.param("fields", "country_currency_desc,exchange_rate,record_date,effective_date"),
                                Parameter.param("filter", "country_currency_desc:in:(Brazil-Real),record_date:lte:2026-03-20"),
                                Parameter.param("sort", "-record_date"),
                                Parameter.param("page[size]", "1")
                        ))
                .respond(response().withStatusCode(500));

        TreasuryExchangeClient client = new TreasuryExchangeClient(
                "http://" + MOCK_SERVER.getHost() + ":" + MOCK_SERVER.getMappedPort(MOCK_SERVER_PORT),
                PATH
        );

        assertThatThrownBy(() -> client.findRate("Brazil-Real", LocalDate.of(2026, 3, 20)))
                .isInstanceOf(ExternalDependencyException.class)
                .hasMessage("Treasury API responded with status 500");
    }
}
