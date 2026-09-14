package com.rahulsmgv.cbs.transaction.application.service;

import com.rahulsmgv.cbs.transaction.application.dto.CreateTransactionRequest;
import com.rahulsmgv.cbs.transaction.application.dto.TransactionResponse;
import com.rahulsmgv.cbs.transaction.application.port.AccountService;
import com.rahulsmgv.cbs.transaction.application.port.TransactionRepository;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;
import com.rahulsmgv.cbs.transaction.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionApplicationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    private TransactionApplicationService service;

    private UUID sourceAccountId;
    private UUID destinationAccountId;

    @BeforeEach
    void setUp() {
        service = new TransactionApplicationService(
                transactionRepository,
                accountService);

        sourceAccountId = UUID.randomUUID();
        destinationAccountId = UUID.randomUUID();

        lenient().when(
                transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        lenient().when(
                transactionRepository.findByTransactionReference(any())).thenReturn(Optional.empty());

        lenient().when(
                accountService.exists(any())).thenReturn(true);

        lenient().when(
                accountService.isActive(any())).thenReturn(true);

        lenient().when(
                accountService.hasSufficientBalance(any(), any())).thenReturn(true);

        lenient().when(
                transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateDebitTransaction() {

        CreateTransactionRequest request = debitRequest();

        TransactionResponse response = service.create(request);

        assertNotNull(response);
        assertNotNull(response.transactionId());

        assertEquals(
                "TXN-DEBIT-001",
                response.transactionReference());

        assertEquals(
                "IDEMP-DEBIT-001",
                response.idempotencyKey());

        assertEquals(
                TransactionType.DEBIT,
                response.transactionType());

        assertEquals(
                TransactionStatus.COMPLETED,
                response.status());

        assertEquals(
                new BigDecimal("100.00"),
                response.amount());

        assertEquals(
                "INR",
                response.currency());

        assertEquals(
                sourceAccountId,
                response.sourceAccountId());

        assertNull(response.destinationAccountId());

        verify(transactionRepository, times(2))
                .save(any(Transaction.class));

        verify(accountService)
                .hasSufficientBalance(
                        sourceAccountId,
                        new BigDecimal("100.00"));
    }

    @Test
    void shouldCreateCreditTransaction() {

        CreateTransactionRequest request = creditRequest();

        TransactionResponse response = service.create(request);

        assertNotNull(response);

        assertEquals(
                TransactionType.CREDIT,
                response.transactionType());

        assertEquals(
                TransactionStatus.COMPLETED,
                response.status());

        assertNull(response.sourceAccountId());

        assertEquals(
                destinationAccountId,
                response.destinationAccountId());

        verify(accountService)
                .exists(destinationAccountId);

        verify(accountService)
                .isActive(destinationAccountId);

        verify(accountService, never())
                .hasSufficientBalance(
                        any(),
                        any());
    }

    @Test
    void shouldCreateTransferTransaction() {

        CreateTransactionRequest request = transferRequest();

        TransactionResponse response = service.create(request);

        assertNotNull(response);

        assertEquals(
                TransactionType.TRANSFER,
                response.transactionType());

        assertEquals(
                TransactionStatus.COMPLETED,
                response.status());

        assertEquals(
                sourceAccountId,
                response.sourceAccountId());

        assertEquals(
                destinationAccountId,
                response.destinationAccountId());

        verify(accountService)
                .hasSufficientBalance(
                        sourceAccountId,
                        new BigDecimal("500.00"));

        verify(accountService)
                .exists(sourceAccountId);

        verify(accountService)
                .exists(destinationAccountId);
    }

    @Test
    void shouldRejectDebitWithoutSourceAccount() {

        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-INVALID-DEBIT",
                "IDEMP-INVALID-DEBIT",
                TransactionType.DEBIT,
                null,
                null,
                new BigDecimal("100.00"),
                "INR",
                "Invalid debit");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request));

        assertEquals(
                "Source account is required for DEBIT",
                exception.getMessage());

        verifyNoInteractions(accountService);
        verify(transactionRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectCreditWithoutDestinationAccount() {

        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-INVALID-CREDIT",
                "IDEMP-INVALID-CREDIT",
                TransactionType.CREDIT,
                null,
                null,
                new BigDecimal("100.00"),
                "INR",
                "Invalid credit");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request));

        assertEquals(
                "Destination account is required for CREDIT",
                exception.getMessage());

        verifyNoInteractions(accountService);
        verify(transactionRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectTransferBetweenSameAccount() {

        UUID accountId = UUID.randomUUID();

        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-SAME-ACCOUNT",
                "IDEMP-SAME-ACCOUNT",
                TransactionType.TRANSFER,
                accountId,
                accountId,
                new BigDecimal("100.00"),
                "INR",
                "Invalid transfer");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request));

        assertEquals(
                "Source and destination accounts must be different",
                exception.getMessage());

        verifyNoInteractions(accountService);
        verify(transactionRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectNonexistentSourceAccount() {

        when(accountService.exists(sourceAccountId))
                .thenReturn(false);

        CreateTransactionRequest request = debitRequest();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request));

        assertEquals(
                "Account not found: " + sourceAccountId,
                exception.getMessage());

        verify(accountService)
                .exists(sourceAccountId);

        verify(accountService, never())
                .isActive(any());

        verify(transactionRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectInactiveSourceAccount() {

        when(accountService.isActive(sourceAccountId))
                .thenReturn(false);

        CreateTransactionRequest request = debitRequest();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.create(request));

        assertEquals(
                "Account is not active: " + sourceAccountId,
                exception.getMessage());

        verify(accountService)
                .exists(sourceAccountId);

        verify(accountService)
                .isActive(sourceAccountId);

        verify(accountService, never())
                .hasSufficientBalance(any(), any());

        verify(transactionRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectInsufficientBalance() {

        when(accountService.hasSufficientBalance(
                sourceAccountId,
                new BigDecimal("100.00"))).thenReturn(false);

        CreateTransactionRequest request = debitRequest();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.create(request));

        assertEquals(
                "Insufficient balance for account: "
                        + sourceAccountId,
                exception.getMessage());

        verify(accountService)
                .hasSufficientBalance(
                        sourceAccountId,
                        new BigDecimal("100.00"));

        verify(transactionRepository, never())
                .save(any());
    }

    @Test
    void shouldReturnExistingTransactionForSameIdempotencyKey() {

        Transaction existingTransaction = Transaction.create(
                "TXN-EXISTING",
                "IDEMP-EXISTING",
                TransactionType.DEBIT,
                sourceAccountId,
                null,
                new com.rahulsmgv.cbs.transaction.domain.model.Money(
                        new BigDecimal("250.00"),
                        "INR"),
                "Existing transaction");

        existingTransaction.startProcessing();
        existingTransaction.complete();

        when(transactionRepository.findByIdempotencyKey(
                "IDEMP-EXISTING")).thenReturn(Optional.of(existingTransaction));

        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-NEW",
                "IDEMP-EXISTING",
                TransactionType.DEBIT,
                sourceAccountId,
                null,
                new BigDecimal("999.00"),
                "INR",
                "Should not create");

        TransactionResponse response = service.create(request);

        assertNotNull(response);

        assertEquals(
                existingTransaction.getTransactionId(),
                response.transactionId());

        assertEquals(
                "TXN-EXISTING",
                response.transactionReference());

        assertEquals(
                new BigDecimal("250.00"),
                response.amount());

        verify(transactionRepository)
                .findByIdempotencyKey("IDEMP-EXISTING");

        verify(transactionRepository, never())
                .save(any());

        verifyNoInteractions(accountService);
    }

    @Test
    void shouldRejectDuplicateTransactionReference() {

        Transaction existingTransaction = Transaction.create(
                "TXN-DUPLICATE",
                "IDEMP-OLD",
                TransactionType.CREDIT,
                null,
                destinationAccountId,
                new com.rahulsmgv.cbs.transaction.domain.model.Money(
                        new BigDecimal("100.00"),
                        "INR"),
                "Existing");

        when(transactionRepository.findByTransactionReference(
                "TXN-DUPLICATE")).thenReturn(Optional.of(existingTransaction));

        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-DUPLICATE",
                "IDEMP-NEW",
                TransactionType.CREDIT,
                null,
                destinationAccountId,
                new BigDecimal("200.00"),
                "INR",
                "Duplicate reference");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.create(request));

        assertEquals(
                "Transaction reference already exists: TXN-DUPLICATE",
                exception.getMessage());

        verify(transactionRepository)
                .findByTransactionReference("TXN-DUPLICATE");

        verify(transactionRepository, never())
                .save(any());

        verifyNoInteractions(accountService);
    }

    @Test
    void shouldGetTransactionById() {

        Transaction transaction = Transaction.create(
                "TXN-GET-ID",
                "IDEMP-GET-ID",
                TransactionType.CREDIT,
                null,
                destinationAccountId,
                new com.rahulsmgv.cbs.transaction.domain.model.Money(
                        new BigDecimal("300.00"),
                        "INR"),
                "Get by ID");

        UUID transactionId = transaction.getTransactionId();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        TransactionResponse response = service.getById(transactionId);

        assertNotNull(response);

        assertEquals(
                transactionId,
                response.transactionId());

        assertEquals(
                "TXN-GET-ID",
                response.transactionReference());

        verify(transactionRepository)
                .findById(transactionId);
    }

    @Test
    void shouldRejectMissingTransactionById() {

        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getById(transactionId));

        assertEquals(
                "Transaction not found: " + transactionId,
                exception.getMessage());
    }

    @Test
    void shouldGetTransactionByIdempotencyKey() {

        Transaction transaction = Transaction.create(
                "TXN-GET-IDEMP",
                "IDEMP-GET-IDEMP",
                TransactionType.DEBIT,
                sourceAccountId,
                null,
                new com.rahulsmgv.cbs.transaction.domain.model.Money(
                        new BigDecimal("150.00"),
                        "INR"),
                "Get by idempotency");

        when(transactionRepository.findByIdempotencyKey(
                "IDEMP-GET-IDEMP")).thenReturn(Optional.of(transaction));

        TransactionResponse response = service.getByIdempotencyKey(
                "IDEMP-GET-IDEMP");

        assertNotNull(response);

        assertEquals(
                transaction.getTransactionId(),
                response.transactionId());

        assertEquals(
                "IDEMP-GET-IDEMP",
                response.idempotencyKey());
    }

    @Test
    void shouldGetTransactionByReference() {

        Transaction transaction = Transaction.create(
                "TXN-GET-REFERENCE",
                "IDEMP-GET-REFERENCE",
                TransactionType.CREDIT,
                null,
                destinationAccountId,
                new com.rahulsmgv.cbs.transaction.domain.model.Money(
                        new BigDecimal("400.00"),
                        "INR"),
                "Get by reference");

        when(transactionRepository.findByTransactionReference(
                "TXN-GET-REFERENCE")).thenReturn(Optional.of(transaction));

        TransactionResponse response = service.getByReference(
                "TXN-GET-REFERENCE");

        assertNotNull(response);

        assertEquals(
                transaction.getTransactionId(),
                response.transactionId());

        assertEquals(
                "TXN-GET-REFERENCE",
                response.transactionReference());
    }

    @Test
    void shouldVerifyTransactionIsCompletedAfterSuccessfulProcessing() {

        CreateTransactionRequest request = transferRequest();

        TransactionResponse response = service.create(request);

        assertEquals(
                TransactionStatus.COMPLETED,
                response.status());

        assertNotNull(response.completedAt());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void shouldSaveTransactionBeforeAndAfterProcessing() {

        CreateTransactionRequest request = debitRequest();

        service.create(request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(2))
                .save(captor.capture());

        var savedTransactions = captor.getAllValues();

        assertEquals(2, savedTransactions.size());

        /*
         * The repository currently returns the same mutable domain
         * object reference. Therefore both captured arguments point
         * to the final state of the transaction.
         *
         * Verify the important contract instead:
         * 1. Transaction is saved twice.
         * 2. The same transaction is persisted.
         * 3. The final state is COMPLETED.
         */
        assertSame(
                savedTransactions.get(0),
                savedTransactions.get(1));

        assertEquals(
                TransactionStatus.COMPLETED,
                savedTransactions.get(1).getStatus());

        assertNotNull(
                savedTransactions.get(1).getCompletedAt());
    }

    private CreateTransactionRequest debitRequest() {

        return new CreateTransactionRequest(
                "TXN-DEBIT-001",
                "IDEMP-DEBIT-001",
                TransactionType.DEBIT,
                sourceAccountId,
                null,
                new BigDecimal("100.00"),
                "INR",
                "ATM withdrawal");
    }

    private CreateTransactionRequest creditRequest() {

        return new CreateTransactionRequest(
                "TXN-CREDIT-001",
                "IDEMP-CREDIT-001",
                TransactionType.CREDIT,
                null,
                destinationAccountId,
                new BigDecimal("200.00"),
                "INR",
                "Cash deposit");
    }

    private CreateTransactionRequest transferRequest() {

        return new CreateTransactionRequest(
                "TXN-TRANSFER-001",
                "IDEMP-TRANSFER-001",
                TransactionType.TRANSFER,
                sourceAccountId,
                destinationAccountId,
                new BigDecimal("500.00"),
                "INR",
                "Account transfer");
    }
}