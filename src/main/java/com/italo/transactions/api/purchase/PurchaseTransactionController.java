package com.italo.transactions.api.purchase;

import com.italo.transactions.api.purchase.requests.CreatePurchaseTransactionRequest;
import com.italo.transactions.api.purchase.requests.GetPurchaseTransactionByCurrencyRequest;
import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.domain.service.PurchaseTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

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
    public String getByIdAndCurrency(
            @PathVariable String id,
            @Valid GetPurchaseTransactionByCurrencyRequest request
    ) {
        return "retrieved";
    }
}
