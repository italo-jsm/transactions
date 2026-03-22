package com.italo.transactions.infrastructure.currency.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(
        String currencyDescription,
        BigDecimal exchangeRate,
        LocalDate recordDate
) {}
