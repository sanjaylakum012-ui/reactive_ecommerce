package com.rahulsmgv.cbs.transaction.domain.model;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionDirection;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {

    private final UUID transactionId;
    private final String transactionReference;
    private final String idempotencyKey;

    private final TransactionType transactionType;

    private final UUID sourceAccountId;
    private final UUID destinationAccountId;

    private final Money amount;

    private TransactionStatus status;

    private String description;
    private String failureReason;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    private Transaction(
            UUID transactionId,
            String transactionReference,
            String idempotencyKey,
            TransactionType transactionType,
            UUID sourceAccountId,
            UUID destinationAccountId,
            Money amount,
            String description) {
        this.transactionId = Objects.requireNonNull(
                transactionId,
                "Transaction ID must not be null");

        this.transactionReference = requireText(
                transactionReference,
                "Transaction reference");

        this.idempotencyKey = requireText(
                idempotencyKey,
                "Idempotency key");

        this.transactionType = Objects.requireNonNull(
                transactionType,
                "Transaction type must not be null");

        this.amount = Objects.requireNonNull(
                amount,
                "Amount must not be null");

        validateAccounts(
                transactionType,
                sourceAccountId,
                destinationAccountId);

        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;

        this.description = normalize(description);

        this.status = TransactionStatus.INITIATED;

        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Transaction create(
            String transactionReference,
            String idempotencyKey,
            TransactionType transactionType,
            UUID sourceAccountId,
            UUID destinationAccountId,
            Money amount,
            String description) {
        return new Transaction(
                UUID.randomUUID(),
                transactionReference,
                idempotencyKey,
                transactionType,
                sourceAccountId,
                destinationAccountId,
                amount,
                description);
    }

    public static Transaction rehydrate(
            UUID transactionId,
            String transactionReference,
            String idempotencyKey,
            TransactionType transactionType,
            UUID sourceAccountId,
            UUID destinationAccountId,
            Money amount,
            String description,
            String failureReason,
            TransactionStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt) {
        Transaction transaction = new Transaction(
                transactionId,
                transactionReference,
                idempotencyKey,
                transactionType,
                sourceAccountId,
                destinationAccountId,
                amount,
                description);

        transaction.failureReason = failureReason;
        transaction.status = Objects.requireNonNull(
                status,
                "Transaction status must not be null");
        transaction.completedAt = completedAt;

        transaction.createdAt = Objects.requireNonNull(
                createdAt,
                "Created timestamp must not be null");
        transaction.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Updated timestamp must not be null");

        return transaction;
    }

    public void startProcessing() {
        ensureStatus(TransactionStatus.INITIATED);

        this.status = TransactionStatus.PROCESSING;
        touch();
    }

    public void complete() {
        ensureStatus(TransactionStatus.PROCESSING);

        this.status = TransactionStatus.COMPLETED;
        this.completedAt = Instant.now();

        touch();
    }

    public void fail(String reason) {
        ensureStatus(
                TransactionStatus.INITIATED,
                TransactionStatus.PROCESSING);

        this.status = TransactionStatus.FAILED;
        this.failureReason = requireText(reason, "Failure reason");

        touch();
    }

    public void reverse() {
        ensureStatus(TransactionStatus.COMPLETED);

        this.status = TransactionStatus.REVERSED;
        touch();
    }

    public boolean isDebit() {
        return transactionType == TransactionType.DEBIT;
    }

    public boolean isCredit() {
        return transactionType == TransactionType.CREDIT;
    }

    public boolean isTransfer() {
        return transactionType == TransactionType.TRANSFER;
    }

    public boolean affectsAccount(
            UUID accountId,
            TransactionDirection direction) {
        Objects.requireNonNull(accountId, "Account ID must not be null");
        Objects.requireNonNull(direction, "Transaction direction must not be null");

        return switch (direction) {
            case DEBIT ->
                accountId.equals(sourceAccountId);

            case CREDIT ->
                accountId.equals(destinationAccountId);
        };
    }

    private void validateAccounts(
            TransactionType transactionType,
            UUID sourceAccountId,
            UUID destinationAccountId) {
        switch (transactionType) {

            case DEBIT -> {
                if (sourceAccountId == null) {
                    throw new IllegalArgumentException(
                            "Source account is required for DEBIT");
                }

                if (destinationAccountId != null) {
                    throw new IllegalArgumentException(
                            "Destination account must not be provided for DEBIT");
                }
            }

            case CREDIT -> {
                if (destinationAccountId == null) {
                    throw new IllegalArgumentException(
                            "Destination account is required for CREDIT");
                }

                if (sourceAccountId != null) {
                    throw new IllegalArgumentException(
                            "Source account must not be provided for CREDIT");
                }
            }

            case TRANSFER -> {
                if (sourceAccountId == null) {
                    throw new IllegalArgumentException(
                            "Source account is required for TRANSFER");
                }

                if (destinationAccountId == null) {
                    throw new IllegalArgumentException(
                            "Destination account is required for TRANSFER");
                }

                if (sourceAccountId.equals(destinationAccountId)) {
                    throw new IllegalArgumentException(
                            "Source and destination accounts must be different");
                }
            }
        }
    }

    private void ensureStatus(TransactionStatus... allowedStatuses) {
        for (TransactionStatus allowedStatus : allowedStatuses) {
            if (status == allowedStatus) {
                return;
            }
        }

        throw new IllegalStateException(
                "Invalid transaction state transition from "
                        + status);
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String requireText(
            String value,
            String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank");
        }

        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}