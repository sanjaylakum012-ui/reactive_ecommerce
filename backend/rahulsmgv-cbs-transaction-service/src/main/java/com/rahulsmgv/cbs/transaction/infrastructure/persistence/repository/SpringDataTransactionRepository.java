package com.rahulsmgv.cbs.transaction.infrastructure.persistence.repository;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;
import com.rahulsmgv.cbs.transaction.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTransactionRepository
        extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByIdempotencyKey(
            String idempotencyKey
    );

    Optional<TransactionEntity> findByTransactionReference(
            String transactionReference
    );

    List<TransactionEntity> findBySourceAccountId(
            UUID sourceAccountId
    );

    List<TransactionEntity> findByDestinationAccountId(
            UUID destinationAccountId
    );

    List<TransactionEntity> findByStatus(
            TransactionStatus status
    );

    List<TransactionEntity> findByTransactionType(
            TransactionType transactionType
    );
}