package com.rahulsmgv.cbs.transaction.application.dto;

import com.rahulsmgv.cbs.transaction.domain.model.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toResponse(
            Transaction transaction
    ) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getTransactionReference(),
                transaction.getIdempotencyKey(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getAmount().amount(),
                transaction.getAmount().currency(),
                transaction.getDescription(),
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getCompletedAt()
        );
    }
}