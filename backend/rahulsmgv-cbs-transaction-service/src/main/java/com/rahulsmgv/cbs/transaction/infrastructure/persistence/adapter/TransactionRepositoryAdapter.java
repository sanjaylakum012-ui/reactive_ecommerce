package com.rahulsmgv.cbs.transaction.infrastructure.persistence.adapter;

import com.rahulsmgv.cbs.transaction.application.port.TransactionRepository;
import com.rahulsmgv.cbs.transaction.domain.model.Money;
import com.rahulsmgv.cbs.transaction.domain.model.Transaction;
import com.rahulsmgv.cbs.transaction.infrastructure.persistence.entity.TransactionEntity;
import com.rahulsmgv.cbs.transaction.infrastructure.persistence.repository.SpringDataTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepositoryAdapter
        implements TransactionRepository {

    private final SpringDataTransactionRepository repository;

    public TransactionRepositoryAdapter(
            SpringDataTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction save(Transaction transaction) {

        TransactionEntity entity = toEntity(transaction);

        TransactionEntity saved = repository.save(entity);

        return toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(
            UUID transactionId) {
        return repository.findById(transactionId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(
            String idempotencyKey) {
        return repository.findByIdempotencyKey(
                idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByTransactionReference(
            String transactionReference) {
        return repository.findByTransactionReference(
                transactionReference)
                .map(this::toDomain);
    }

    private TransactionEntity toEntity(
            Transaction transaction) {
        return new TransactionEntity(
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
                transaction.getCompletedAt());
    }

    private Transaction toDomain(
            TransactionEntity entity) {
        return Transaction.rehydrate(
                entity.getTransactionId(),
                entity.getTransactionReference(),
                entity.getIdempotencyKey(),
                entity.getTransactionType(),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()),
                entity.getDescription(),
                entity.getFailureReason(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCompletedAt());
    }
}