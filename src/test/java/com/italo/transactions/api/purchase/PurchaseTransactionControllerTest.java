package com.italo.transactions.api.purchase;

import com.italo.transactions.api.GlobalExceptionHandler;
import com.italo.transactions.domain.exception.EntityNotFoundException;
import com.italo.transactions.domain.exception.ExchangeRateNotFoundException;
import com.italo.transactions.domain.exception.ExternalDependencyTimeoutException;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.service.PurchaseTransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseTransactionControllerTest {

    private final ReloadableResourceBundleMessageSource messageSource = messageSource();
    private final LocalValidatorFactoryBean validator = validator();

    private final PurchaseTransactionService service = Mockito.mock(PurchaseTransactionService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PurchaseTransactionController(service))
            .setControllerAdvice(new GlobalExceptionHandler(messageSource))
            .setValidator(validator)
            .build();

    @Test
    void shouldCreatePurchaseTransactionAndReturnCreatedLocation() throws Exception {
        UUID generatedId = UUID.randomUUID();
        ArgumentCaptor<com.italo.transactions.domain.model.PurchaseTransaction> transactionCaptor =
                ArgumentCaptor.forClass(com.italo.transactions.domain.model.PurchaseTransaction.class);

        when(service.create(any())).thenReturn(generatedId);

        mockMvc.perform(post("/purchase-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Notebook",
                                  "transactionDate": "2026-03-20",
                                  "amount": 1999.90
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/purchase-transactions/" + generatedId));

        verify(service).create(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().description()).isEqualTo("Notebook");
        assertThat(transactionCaptor.getValue().transactionDate()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(transactionCaptor.getValue().purchaseAmount()).isEqualByComparingTo("1999.90");
    }

    @Test
    void shouldRoundPurchaseAmountToNearestCentWhenCreatingTransaction() throws Exception {
        UUID generatedId = UUID.randomUUID();
        ArgumentCaptor<com.italo.transactions.domain.model.PurchaseTransaction> transactionCaptor =
                ArgumentCaptor.forClass(com.italo.transactions.domain.model.PurchaseTransaction.class);

        when(service.create(any())).thenReturn(generatedId);

        mockMvc.perform(post("/purchase-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Notebook",
                                  "transactionDate": "2026-03-20",
                                  "amount": 1999.995
                                }
                                """))
                .andExpect(status().isCreated());

        verify(service).create(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().purchaseAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    void shouldReturnTransactionConvertedToCountryCurrency() throws Exception {
        UUID transactionId = UUID.randomUUID();
        CountryPurchaseTransaction countryPurchaseTransaction = new CountryPurchaseTransaction(
                transactionId,
                "Notebook",
                LocalDate.of(2026, 3, 20),
                new BigDecimal("100.00"),
                new BigDecimal("525.00"),
                new BigDecimal("5.25"),
                LocalDate.of(2026, 3, 19)
        );

        when(service.getTransactionInCountryCurrency(transactionId, "Brazil")).thenReturn(countryPurchaseTransaction);

        mockMvc.perform(get("/purchase-transactions/{id}", transactionId)
                        .param("country", "Brazil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseId").value(transactionId.toString()))
                .andExpect(jsonPath("$.description").value("Notebook"))
                .andExpect(jsonPath("$.transactionDate").value("2026-03-20"))
                .andExpect(jsonPath("$.purchaseAmount").value(100.00))
                .andExpect(jsonPath("$.exchangeRate").value(5.25))
                .andExpect(jsonPath("$.convertedPurchasedAmount").value(525.00));
    }

    @Test
    void shouldReturnFieldErrorsWhenCreatePayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/purchase-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "",
                                  "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("amount must be greater than zero"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("description"))
                .andExpect(jsonPath("$.fieldErrors[1].message").value("description is required"))
                .andExpect(jsonPath("$.fieldErrors[2].field").value("transactionDate"))
                .andExpect(jsonPath("$.fieldErrors[2].message").value("transactionDate is required"));
    }

    @Test
    void shouldReturnFieldErrorWhenTransactionDateHasInvalidFormat() throws Exception {
        mockMvc.perform(post("/purchase-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Notebook",
                                  "transactionDate": "2026-03-203",
                                  "amount": 0.01
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("transactionDate"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("transactionDate must be a valid date in yyyy-MM-dd format"));
    }

    @Test
    void shouldReturnFieldErrorWhenTransactionDateIsImpossible() throws Exception {
        mockMvc.perform(post("/purchase-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Notebook",
                                  "transactionDate": "2026-02-30",
                                  "amount": 0.01
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("transactionDate"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("transactionDate must be a valid date in yyyy-MM-dd format"));
    }

    @Test
    void shouldReturnNotFoundWhenPurchaseTransactionDoesNotExist() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(service.getTransactionInCountryCurrency(transactionId, "Brazil"))
                .thenThrow(new EntityNotFoundException("Purchase transaction not found"));

        mockMvc.perform(get("/purchase-transactions/{id}", transactionId)
                        .param("country", "Brazil"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUnprocessableEntityWhenExchangeRateIsNotAvailable() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(service.getTransactionInCountryCurrency(eq(transactionId), eq("Brazil")))
                .thenThrow(new ExchangeRateNotFoundException("Exchange rate not found within 6 months for the specified country"));

        mockMvc.perform(get("/purchase-transactions/{id}", transactionId)
                        .param("country", "Brazil"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Exchange rate not found within 6 months for the specified country"))
                .andExpect(jsonPath("$.message").value("Exchange rate not found within 6 months for the specified country"));
    }

    @Test
    void shouldReturnGatewayTimeoutWhenTreasuryTakesTooLongToRespond() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(service.getTransactionInCountryCurrency(eq(transactionId), eq("Brazil")))
                .thenThrow(new ExternalDependencyTimeoutException("Treasury API did not respond within 3 seconds", null));

        mockMvc.perform(get("/purchase-transactions/{id}", transactionId)
                        .param("country", "Brazil"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.error").value("Gateway Timeout"))
                .andExpect(jsonPath("$.message").value("Treasury API did not respond within 3 seconds"));
    }

    @Test
    void shouldReturnBadRequestWhenIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/purchase-transactions/{id}", "not-a-uuid")
                        .param("country", "Brazil"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("id must be a valid UUID"));
    }

    @Test
    void shouldReturnBadRequestWhenCountryIsBlank() throws Exception {
        UUID transactionId = UUID.randomUUID();

        mockMvc.perform(get("/purchase-transactions/{id}", transactionId)
                        .param("country", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("country"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("country is required"));
    }

    private ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    private LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(messageSource);
        validatorFactoryBean.afterPropertiesSet();
        return validatorFactoryBean;
    }
}
