package com.rahulsmgv.cbs.transaction.application.port;

import com.rahulsmgv.cbs.transaction.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByTransactionReference(
            String transactionReference
    );
}