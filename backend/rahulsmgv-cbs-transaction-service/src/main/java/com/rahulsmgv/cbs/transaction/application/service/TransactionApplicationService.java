package com.rahulsmgv.cbs.transaction.application.service;

import com.rahulsmgv.cbs.transaction.application.dto.CreateTransactionRequest;
import com.rahulsmgv.cbs.transaction.application.dto.TransactionMapper;
import com.rahulsmgv.cbs.transaction.application.dto.TransactionResponse;
import com.rahulsmgv.cbs.transaction.application.port.AccountService;
import com.rahulsmgv.cbs.transaction.application.port.TransactionRepository;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;
import com.rahulsmgv.cbs.transaction.domain.model.Money;
import com.rahulsmgv.cbs.transaction.domain.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class TransactionApplicationService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionApplicationService(
            TransactionRepository transactionRepository,
            AccountService accountService
    ) {
        this.transactionRepository = Objects.requireNonNull(
                transactionRepository,
                "Transaction repository must not be null"
        );

        this.accountService = Objects.requireNonNull(
                accountService,
                "Account service must not be null"
        );
    }

    public TransactionResponse create(
            CreateTransactionRequest request
    ) {
        Objects.requireNonNull(
                request,
                "Create transaction request must not be null"
        );

        /*
         * Idempotency protection.
         *
         * If the same idempotency key was already processed,
         * return the existing transaction instead of creating
         * another transaction.
         */
        var existingTransaction =
                transactionRepository.findByIdempotencyKey(
                        request.idempotencyKey()
                );

        if (existingTransaction.isPresent()) {
            return TransactionMapper.toResponse(
                    existingTransaction.get()
            );
        }

        /*
         * Transaction reference must also be unique.
         */
        var existingReference =
                transactionRepository.findByTransactionReference(
                        request.transactionReference()
                );

        if (existingReference.isPresent()) {
            throw new IllegalStateException(
                    "Transaction reference already exists: "
                            + request.transactionReference()
            );
        }

        validateAccounts(request);

        /*
         * Validate source account for transactions
         * that debit an account.
         */
        if (requiresSourceAccount(request.transactionType())) {

            validateAccountExists(
                    request.sourceAccountId()
            );

            validateAccountActive(
                    request.sourceAccountId()
            );

            validateSufficientBalance(
                    request.sourceAccountId(),
                    request.amount()
            );
        }

        /*
         * Validate destination account for transactions
         * that credit an account.
         */
        if (requiresDestinationAccount(request.transactionType())) {

            validateAccountExists(
                    request.destinationAccountId()
            );

            validateAccountActive(
                    request.destinationAccountId()
            );
        }

        Money money = new Money(
                request.amount(),
                request.currency()
        );

        Transaction transaction = Transaction.create(
                request.transactionReference(),
                request.idempotencyKey(),
                request.transactionType(),
                request.sourceAccountId(),
                request.destinationAccountId(),
                money,
                request.description()
        );

        transactionRepository.save(transaction);

        try {

            transaction.startProcessing();

            transaction.complete();

            transactionRepository.save(transaction);

            return TransactionMapper.toResponse(transaction);

        } catch (RuntimeException exception) {

            transaction.fail(
                    exception.getMessage() != null
                            ? exception.getMessage()
                            : "Transaction processing failed"
            );

            transactionRepository.save(transaction);

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(
            UUID transactionId
    ) {
        Objects.requireNonNull(
                transactionId,
                "Transaction ID must not be null"
        );

        Transaction transaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Transaction not found: "
                                                + transactionId
                                )
                        );

        return TransactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByIdempotencyKey(
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency key must not be blank"
            );
        }

        Transaction transaction =
                transactionRepository.findByIdempotencyKey(
                                idempotencyKey
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Transaction not found for idempotency key: "
                                                + idempotencyKey
                                )
                        );

        return TransactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(
            String transactionReference
    ) {
        if (transactionReference == null
                || transactionReference.isBlank()) {

            throw new IllegalArgumentException(
                    "Transaction reference must not be blank"
            );
        }

        Transaction transaction =
                transactionRepository.findByTransactionReference(
                                transactionReference
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Transaction not found for reference: "
                                                + transactionReference
                                )
                        );

        return TransactionMapper.toResponse(transaction);
    }

    private void validateAccounts(
            CreateTransactionRequest request
    ) {
        TransactionType type = request.transactionType();

        switch (type) {

            case DEBIT -> {
                if (request.sourceAccountId() == null) {
                    throw new IllegalArgumentException(
                            "Source account is required for DEBIT"
                    );
                }

                if (request.destinationAccountId() != null) {
                    throw new IllegalArgumentException(
                            "Destination account must not be provided for DEBIT"
                    );
                }
            }

            case CREDIT -> {
                if (request.destinationAccountId() == null) {
                    throw new IllegalArgumentException(
                            "Destination account is required for CREDIT"
                    );
                }

                if (request.sourceAccountId() != null) {
                    throw new IllegalArgumentException(
                            "Source account must not be provided for CREDIT"
                    );
                }
            }

            case TRANSFER -> {
                if (request.sourceAccountId() == null) {
                    throw new IllegalArgumentException(
                            "Source account is required for TRANSFER"
                    );
                }

                if (request.destinationAccountId() == null) {
                    throw new IllegalArgumentException(
                            "Destination account is required for TRANSFER"
                    );
                }

                if (request.sourceAccountId()
                        .equals(request.destinationAccountId())) {

                    throw new IllegalArgumentException(
                            "Source and destination accounts must be different"
                    );
                }
            }
        }
    }

    private boolean requiresSourceAccount(
            TransactionType transactionType
    ) {
        return transactionType == TransactionType.DEBIT
                || transactionType == TransactionType.TRANSFER;
    }

    private boolean requiresDestinationAccount(
            TransactionType transactionType
    ) {
        return transactionType == TransactionType.CREDIT
                || transactionType == TransactionType.TRANSFER;
    }

    private void validateAccountExists(
            UUID accountId
    ) {
        if (!accountService.exists(accountId)) {
            throw new IllegalArgumentException(
                    "Account not found: " + accountId
            );
        }
    }

    private void validateAccountActive(
            UUID accountId
    ) {
        if (!accountService.isActive(accountId)) {
            throw new IllegalStateException(
                    "Account is not active: " + accountId
            );
        }
    }

    private void validateSufficientBalance(
            UUID accountId,
            java.math.BigDecimal amount
    ) {
        if (!accountService.hasSufficientBalance(
                accountId,
                amount
        )) {
            throw new IllegalStateException(
                    "Insufficient balance for account: "
                            + accountId
            );
        }
    }
}