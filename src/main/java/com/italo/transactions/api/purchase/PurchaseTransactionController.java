package com.italo.transactions.api.purchase;

import com.italo.transactions.api.purchase.requests.CreatePurchaseTransactionRequest;
import com.italo.transactions.api.purchase.requests.GetPurchaseTransactionByCurrencyRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase-transactions")
@Validated
public class PurchaseTransactionController {

    @PostMapping
    public String create(@Valid @RequestBody CreatePurchaseTransactionRequest request) {
        return "created";
    }

    @GetMapping("/{id}")
    public String getByIdAndCurrency(
            @PathVariable String id,
            @Valid GetPurchaseTransactionByCurrencyRequest request
    ) {
        return "retrieved";
    }
}
