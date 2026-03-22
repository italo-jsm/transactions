package com.italo.transactions.infrastructure.currency.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TreasuryRateDto(

        @JsonProperty("country_currency_desc")
        String countryCurrencyDesc,

        @JsonProperty("exchange_rate")
        String exchangeRate,

        @JsonProperty("record_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate recordDate,

        @JsonProperty("effective_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate effectiveDate
) {}
