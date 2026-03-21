package com.italo.transactions.api.purchase;

import com.italo.transactions.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseTransactionControllerTest {

    private final ReloadableResourceBundleMessageSource messageSource = messageSource();
    private final LocalValidatorFactoryBean validator = validator();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PurchaseTransactionController())
            .setControllerAdvice(new GlobalExceptionHandler(messageSource))
            .setValidator(validator)
            .build();

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
    void shouldReturnFieldErrorsWhenCurrencyCodeIsInvalid() throws Exception {
        mockMvc.perform(get("/purchase-transactions/123")
                        .param("currencyCode", "br"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currencyCode"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("currencyCode must be a valid 3-letter ISO code"));
    }

    @Test
    void shouldReturnFieldErrorWhenTransactionDateHasInvalidFormat() throws Exception {
        mockMvc.perform(post("/purchase-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "TesteTeste",
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
