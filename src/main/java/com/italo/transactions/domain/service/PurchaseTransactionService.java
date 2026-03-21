package com.italo.transactions.domain.service;

import com.italo.transactions.domain.model.PurchaseTransaction;
import com.italo.transactions.domain.repository.PurchaseTransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseTransactionService {

    private final PurchaseTransactionsRepository purchaseTransactionsRepository;

    public UUID create(PurchaseTransaction purchaseTransaction){
        return purchaseTransactionsRepository.savePurchaseTransaction(purchaseTransaction);
    }
}
