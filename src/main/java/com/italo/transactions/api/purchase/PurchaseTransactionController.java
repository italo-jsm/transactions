package com.italo.transactions.api.purchase;

import com.italo.transactions.api.InvalidRequestException;
import com.italo.transactions.api.purchase.requests.CreatePurchaseTransactionRequest;
import com.italo.transactions.api.purchase.responses.GetPurchaseTransactionResponse;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.domain.service.PurchaseTransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

@RestController
@RequestMapping("/purchase-transactions")
@Validated
@RequiredArgsConstructor
public class PurchaseTransactionController {

    private static final DateTimeFormatter STRICT_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final PurchaseTransactionService purchaseTransactionService;

    @PostMapping
    public ResponseEntity<UUID> create(@Valid @RequestBody CreatePurchaseTransactionRequest request) {
        UUID uuid = purchaseTransactionService
                .create(
                        PurchaseTransaction
                                .create(
                                        UUID.randomUUID(),
                                        request.description(),
                                        parseTransactionDate(request.transactionDate()),
                                        request.amount()
                                )
                );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(uuid)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetPurchaseTransactionResponse> getByIdAndCurrency(
            @PathVariable UUID id,
            @RequestParam @NotBlank(message = "{validation.purchase.country.required}") String country
    ) {
        String normalizedCountry = country.trim();
        if (normalizedCountry.isEmpty()) {
            throw new InvalidRequestException("country", "country is required");
        }

        CountryPurchaseTransaction transactionInCountryCurrency = purchaseTransactionService.getTransactionInCountryCurrency(id, normalizedCountry);
        return ResponseEntity.ok(GetPurchaseTransactionResponse.create(transactionInCountryCurrency));
    }

    private LocalDate parseTransactionDate(String transactionDate) {
        try {
            return LocalDate.parse(transactionDate, STRICT_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new InvalidRequestException("transactionDate", "transactionDate must be a valid date in yyyy-MM-dd format");
        }
    }
}
