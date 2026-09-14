package com.rahulsmgv.cbs.transaction.infrastructure.persistence;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;
import com.rahulsmgv.cbs.transaction.domain.model.Money;
import com.rahulsmgv.cbs.transaction.domain.model.Transaction;
import com.rahulsmgv.cbs.transaction.infrastructure.persistence.adapter.TransactionRepositoryAdapter;
import com.rahulsmgv.cbs.transaction.infrastructure.persistence.repository.SpringDataTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.math.BigDecimal;
import java.util.UUID;
import com.rahulsmgv.cbs.transaction.application.port.AccountService;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TransactionRepositoryIntegrationTest {

        @MockBean
        private AccountService accountService;

        @Autowired
        private SpringDataTransactionRepository repository;

        @Autowired
        private TransactionRepositoryAdapter transactionRepositoryAdapter;

        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
                        .withDatabaseName("transaction_test")
                        .withUsername("test")
                        .withPassword("test");

        private UUID sourceAccountId;
        private UUID destinationAccountId;

        @BeforeEach
        void setUp() {
                repository.deleteAll();

                sourceAccountId = UUID.randomUUID();
                destinationAccountId = UUID.randomUUID();
        }

        @Test
        void shouldPersistAndRetrieveTransaction() {

                Transaction transaction = Transaction.create(
                                "TXN-100001",
                                "IDEMP-100001",
                                TransactionType.TRANSFER,
                                sourceAccountId,
                                destinationAccountId,
                                new Money(
                                                new BigDecimal("1250.50"),
                                                "INR"),
                                "Test transfer");

                Transaction saved = transactionRepositoryAdapter.save(transaction);

                assertNotNull(saved);
                assertEquals(
                                transaction.getTransactionId(),
                                saved.getTransactionId());

                Transaction retrieved = transactionRepositoryAdapter
                                .findById(transaction.getTransactionId())
                                .orElseThrow();

                assertEquals(
                                transaction.getTransactionId(),
                                retrieved.getTransactionId());

                assertEquals(
                                "TXN-100001",
                                retrieved.getTransactionReference());

                assertEquals(
                                "IDEMP-100001",
                                retrieved.getIdempotencyKey());

                assertEquals(
                                TransactionType.TRANSFER,
                                retrieved.getTransactionType());

                assertEquals(
                                TransactionStatus.INITIATED,
                                retrieved.getStatus());

                assertEquals(
                                sourceAccountId,
                                retrieved.getSourceAccountId());

                assertEquals(
                                destinationAccountId,
                                retrieved.getDestinationAccountId());

                assertEquals(
                                0,
                                new BigDecimal("1250.50")
                                                .compareTo(retrieved.getAmount().amount()));

                assertEquals(
                                "INR",
                                retrieved.getAmount().currency());

                assertEquals(
                                "Test transfer",
                                retrieved.getDescription());

                assertNotNull(retrieved.getCreatedAt());
                assertNotNull(retrieved.getUpdatedAt());
        }

        @Test
        void shouldFindByIdempotencyKey() {

                Transaction transaction = Transaction.create(
                                "TXN-100002",
                                "IDEMP-100002",
                                TransactionType.DEBIT,
                                sourceAccountId,
                                null,
                                new Money(
                                                new BigDecimal("500.00"),
                                                "INR"),
                                "Debit transaction");

                transactionRepositoryAdapter.save(transaction);

                Transaction retrieved = transactionRepositoryAdapter
                                .findByIdempotencyKey("IDEMP-100002")
                                .orElseThrow();

                assertEquals(
                                transaction.getTransactionId(),
                                retrieved.getTransactionId());

                assertEquals(
                                "IDEMP-100002",
                                retrieved.getIdempotencyKey());
        }

        @Test
        void shouldFindByTransactionReference() {

                Transaction transaction = Transaction.create(
                                "TXN-100003",
                                "IDEMP-100003",
                                TransactionType.CREDIT,
                                null,
                                destinationAccountId,
                                new Money(
                                                new BigDecimal("750.25"),
                                                "INR"),
                                "Credit transaction");

                transactionRepositoryAdapter.save(transaction);

                Transaction retrieved = transactionRepositoryAdapter
                                .findByTransactionReference(
                                                "TXN-100003")
                                .orElseThrow();

                assertEquals(
                                transaction.getTransactionId(),
                                retrieved.getTransactionId());

                assertEquals(
                                "TXN-100003",
                                retrieved.getTransactionReference());
        }

        @Test
        void shouldPersistCompletedTransactionState() {

                Transaction transaction = Transaction.create(
                                "TXN-100004",
                                "IDEMP-100004",
                                TransactionType.DEBIT,
                                sourceAccountId,
                                null,
                                new Money(
                                                new BigDecimal("100.00"),
                                                "INR"),
                                "Completed debit");

                transaction.startProcessing();
                transaction.complete();

                transactionRepositoryAdapter.save(transaction);

                Transaction retrieved = transactionRepositoryAdapter
                                .findById(transaction.getTransactionId())
                                .orElseThrow();

                assertEquals(
                                TransactionStatus.COMPLETED,
                                retrieved.getStatus());

                assertNotNull(retrieved.getCompletedAt());
        }

        @Test
        void shouldPersistFailedTransactionState() {

                Transaction transaction = Transaction.create(
                                "TXN-100005",
                                "IDEMP-100005",
                                TransactionType.DEBIT,
                                sourceAccountId,
                                null,
                                new Money(
                                                new BigDecimal("200.00"),
                                                "INR"),
                                "Failed debit");

                transaction.fail("Insufficient balance");

                transactionRepositoryAdapter.save(transaction);

                Transaction retrieved = transactionRepositoryAdapter
                                .findById(transaction.getTransactionId())
                                .orElseThrow();

                assertEquals(
                                TransactionStatus.FAILED,
                                retrieved.getStatus());

                assertEquals(
                                "Insufficient balance",
                                retrieved.getFailureReason());

                assertNull(retrieved.getCompletedAt());
        }

        @Test
        void shouldEnforceUniqueTransactionReference() {

                Transaction first = Transaction.create(
                                "TXN-DUPLICATE",
                                "IDEMP-100006",
                                TransactionType.DEBIT,
                                sourceAccountId,
                                null,
                                new Money(
                                                new BigDecimal("100.00"),
                                                "INR"),
                                null);

                Transaction second = Transaction.create(
                                "TXN-DUPLICATE",
                                "IDEMP-100007",
                                TransactionType.DEBIT,
                                sourceAccountId,
                                null,
                                new Money(
                                                new BigDecimal("200.00"),
                                                "INR"),
                                null);

                transactionRepositoryAdapter.save(first);

                assertThrows(
                                RuntimeException.class,
                                () -> transactionRepositoryAdapter.save(second));
        }

        @Test
        void shouldEnforceUniqueIdempotencyKey() {

                Transaction first = Transaction.create(
                                "TXN-100008",
                                "IDEMP-DUPLICATE",
                                TransactionType.DEBIT,
                                sourceAccountId,
                                null,
                                new Money(
                                                new BigDecimal("100.00"),
                                                "INR"),
                                null);

                Transaction second = Transaction.create(
                                "TXN-100009",
                                "IDEMP-DUPLICATE",
                                TransactionType.DEBIT,
                                sourceAccountId,
                                null,
                                new Money(
                                                new BigDecimal("200.00"),
                                                "INR"),
                                null);

                transactionRepositoryAdapter.save(first);

                assertThrows(
                                RuntimeException.class,
                                () -> transactionRepositoryAdapter.save(second));
        }

        @DynamicPropertySource
        static void configurePostgres(DynamicPropertyRegistry registry) {

                registry.add(
                                "spring.datasource.url",
                                postgres::getJdbcUrl);

                registry.add(
                                "spring.datasource.username",
                                postgres::getUsername);

                registry.add(
                                "spring.datasource.password",
                                postgres::getPassword);
        }
}