package com.italo.transactions.api.purchase;

import com.italo.transactions.api.InvalidRequestException;
import com.italo.transactions.api.purchase.requests.CreatePurchaseTransactionRequest;
import com.italo.transactions.api.purchase.responses.CreatePurchaseTransactionResponse;
import com.italo.transactions.api.purchase.responses.GetPurchaseTransactionResponse;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.service.PurchaseTransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/purchase-transactions")
@Validated
@RequiredArgsConstructor
public class PurchaseTransactionController {

    private final PurchaseTransactionService purchaseTransactionService;

    @PostMapping
    public ResponseEntity<CreatePurchaseTransactionResponse> create(@Valid @RequestBody CreatePurchaseTransactionRequest request) {
        UUID uuid = purchaseTransactionService.create(request.toDomain(UUID.randomUUID()));
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(uuid)
                .toUri();

        return ResponseEntity.created(location).body(CreatePurchaseTransactionResponse.from(uuid));
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
}
