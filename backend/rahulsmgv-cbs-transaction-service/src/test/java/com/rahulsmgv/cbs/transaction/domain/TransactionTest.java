package com.rahulsmgv.cbs.transaction.domain;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;
import com.rahulsmgv.cbs.transaction.domain.model.Money;
import com.rahulsmgv.cbs.transaction.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private static final UUID SOURCE_ACCOUNT =
            UUID.randomUUID();

    private static final UUID DESTINATION_ACCOUNT =
            UUID.randomUUID();

    private static Money money() {
        return new Money(
                new BigDecimal("1000.00"),
                "INR"
        );
    }

    @Test
    void shouldCreateDebitTransaction() {

        Transaction transaction = Transaction.create(
                "TXN-100001",
                "IDEMP-100001",
                TransactionType.DEBIT,
                SOURCE_ACCOUNT,
                null,
                money(),
                "Cash withdrawal"
        );

        assertNotNull(transaction.getTransactionId());
        assertEquals(
                "TXN-100001",
                transaction.getTransactionReference()
        );
        assertEquals(
                TransactionType.DEBIT,
                transaction.getTransactionType()
        );
        assertEquals(
                TransactionStatus.INITIATED,
                transaction.getStatus()
        );
    }

    @Test
    void shouldCreateCreditTransaction() {

        Transaction transaction = Transaction.create(
                "TXN-100002",
                "IDEMP-100002",
                TransactionType.CREDIT,
                null,
                DESTINATION_ACCOUNT,
                money(),
                "Cash deposit"
        );

        assertEquals(
                TransactionType.CREDIT,
                transaction.getTransactionType()
        );

        assertEquals(
                DESTINATION_ACCOUNT,
                transaction.getDestinationAccountId()
        );
    }

    @Test
    void shouldCreateTransferTransaction() {

        Transaction transaction = Transaction.create(
                "TXN-100003",
                "IDEMP-100003",
                TransactionType.TRANSFER,
                SOURCE_ACCOUNT,
                DESTINATION_ACCOUNT,
                money(),
                "Account transfer"
        );

        assertTrue(transaction.isTransfer());

        assertEquals(
                SOURCE_ACCOUNT,
                transaction.getSourceAccountId()
        );

        assertEquals(
                DESTINATION_ACCOUNT,
                transaction.getDestinationAccountId()
        );
    }

    @Test
    void shouldRejectTransferToSameAccount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> Transaction.create(
                        "TXN-100004",
                        "IDEMP-100004",
                        TransactionType.TRANSFER,
                        SOURCE_ACCOUNT,
                        SOURCE_ACCOUNT,
                        money(),
                        "Invalid transfer"
                )
        );
    }

    @Test
    void shouldProcessAndCompleteTransaction() {

        Transaction transaction = Transaction.create(
                "TXN-100005",
                "IDEMP-100005",
                TransactionType.DEBIT,
                SOURCE_ACCOUNT,
                null,
                money(),
                "Payment"
        );

        transaction.startProcessing();

        assertEquals(
                TransactionStatus.PROCESSING,
                transaction.getStatus()
        );

        transaction.complete();

        assertEquals(
                TransactionStatus.COMPLETED,
                transaction.getStatus()
        );

        assertNotNull(transaction.getCompletedAt());
    }

    @Test
    void shouldFailTransaction() {

        Transaction transaction = Transaction.create(
                "TXN-100006",
                "IDEMP-100006",
                TransactionType.DEBIT,
                SOURCE_ACCOUNT,
                null,
                money(),
                "Payment"
        );

        transaction.startProcessing();

        transaction.fail("Insufficient balance");

        assertEquals(
                TransactionStatus.FAILED,
                transaction.getStatus()
        );

        assertEquals(
                "Insufficient balance",
                transaction.getFailureReason()
        );
    }

    @Test
    void shouldReverseCompletedTransaction() {

        Transaction transaction = Transaction.create(
                "TXN-100007",
                "IDEMP-100007",
                TransactionType.DEBIT,
                SOURCE_ACCOUNT,
                null,
                money(),
                "Payment"
        );

        transaction.startProcessing();
        transaction.complete();
        transaction.reverse();

        assertEquals(
                TransactionStatus.REVERSED,
                transaction.getStatus()
        );
    }

    @Test
    void shouldRejectInvalidStatusTransition() {

        Transaction transaction = Transaction.create(
                "TXN-100008",
                "IDEMP-100008",
                TransactionType.DEBIT,
                SOURCE_ACCOUNT,
                null,
                money(),
                "Payment"
        );

        assertThrows(
                IllegalStateException.class,
                transaction::complete
        );
    }

    @Test
    void shouldRejectDebitWithoutSourceAccount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> Transaction.create(
                        "TXN-100009",
                        "IDEMP-100009",
                        TransactionType.DEBIT,
                        null,
                        null,
                        money(),
                        "Invalid debit"
                )
        );
    }

    @Test
    void shouldRejectCreditWithoutDestinationAccount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> Transaction.create(
                        "TXN-100010",
                        "IDEMP-100010",
                        TransactionType.CREDIT,
                        null,
                        null,
                        money(),
                        "Invalid credit"
                )
        );
    }
}