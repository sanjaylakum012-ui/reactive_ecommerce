package com.rahulsmgv.cbs.transaction.infrastructure.persistence.entity;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transaction_reference",
                        columnNames = "transaction_reference"
                ),
                @UniqueConstraint(
                        name = "uk_transaction_idempotency_key",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_transaction_source_account",
                        columnList = "source_account_id"
                ),
                @Index(
                        name = "idx_transaction_destination_account",
                        columnList = "destination_account_id"
                ),
                @Index(
                        name = "idx_transaction_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_transaction_created_at",
                        columnList = "created_at"
                )
        }
)
public class TransactionEntity {

    @Id
    @Column(
            name = "transaction_id",
            nullable = false,
            updatable = false
    )
    private UUID transactionId;

    @Column(
            name = "transaction_reference",
            nullable = false,
            length = 100
    )
    private String transactionReference;

    @Column(
            name = "idempotency_key",
            nullable = false,
            length = 150
    )
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_type",
            nullable = false,
            length = 20
    )
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private TransactionStatus status;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal amount;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TransactionEntity() {
        // Required by JPA
    }

    public TransactionEntity(
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
        this.transactionId = transactionId;
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.transactionType = transactionType;
        this.status = status;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
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