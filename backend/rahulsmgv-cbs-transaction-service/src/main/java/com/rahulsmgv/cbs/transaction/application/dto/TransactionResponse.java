package com.rahulsmgv.cbs.transaction.application.dto;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(

        UUID transactionId,

        String transactionReference,

        String idempotencyKey,

        TransactionType transactionType,

        TransactionStatus status,

        UUID sourceAccountId,

        UUID destinationAccountId,

        BigDecimal amount,

        String currency,

        String description,

        String failureReason,

        Instant createdAt,

        Instant updatedAt,

        Instant completedAt
) {
}