package com.italo.transactions.api.purchase;

import com.italo.transactions.api.purchase.requests.CreatePurchaseTransactionRequest;
import com.italo.transactions.api.purchase.responses.GetPurchaseTransactionResponse;
import com.italo.transactions.domain.model.CountryPurchaseTransaction;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.domain.service.PurchaseTransactionService;
import jakarta.validation.Valid;
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
    public ResponseEntity<UUID> create(@Valid @RequestBody CreatePurchaseTransactionRequest request) {
        UUID uuid = purchaseTransactionService
                .create(
                        PurchaseTransaction
                                .create(
                                        UUID.randomUUID(),
                                        request.description(),
                                        request.transactionDate(),
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
            @PathVariable String id,
            @RequestParam String country
    ) {
        CountryPurchaseTransaction transactionInCountryCurrency = purchaseTransactionService.getTransactionInCountryCurrency(UUID.fromString(id), country);
        return ResponseEntity.ok(GetPurchaseTransactionResponse.create(transactionInCountryCurrency));
    }
}
