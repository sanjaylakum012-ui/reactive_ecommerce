package com.rahulsmgv.cbs.account.application.service;

import com.rahulsmgv.cbs.account.application.dto.AccountResponse;
import com.rahulsmgv.cbs.account.application.dto.CreateAccountCommand;
import com.rahulsmgv.cbs.account.application.port.AccountIdGenerator;
import com.rahulsmgv.cbs.account.application.port.AccountNumberGenerator;
import com.rahulsmgv.cbs.account.application.port.AccountRepository;
import com.rahulsmgv.cbs.account.domain.model.Account;
import com.rahulsmgv.cbs.account.domain.valueobject.AccountId;
import com.rahulsmgv.cbs.account.domain.valueobject.AccountNumber;
import com.rahulsmgv.cbs.account.domain.valueobject.Currency;
import com.rahulsmgv.cbs.account.domain.valueobject.CustomerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rahulsmgv.cbs.account.exception.AccountNotFoundException;
import com.rahulsmgv.cbs.account.exception.DuplicateAccountException;

@Service
@Transactional
public class AccountApplicationService {

    private static final Logger log =
            LoggerFactory.getLogger(AccountApplicationService.class);

    private final AccountRepository accountRepository;
    private final AccountIdGenerator accountIdGenerator;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountApplicationService(
            AccountRepository accountRepository,
            AccountIdGenerator accountIdGenerator,
            AccountNumberGenerator accountNumberGenerator) {

        this.accountRepository = accountRepository;
        this.accountIdGenerator = accountIdGenerator;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    /**
     * Create account flow:
     * 1. Validate duplicate account rules
     * 2. Generate new identifiers
     * 3. Build the domain aggregate
     * 4. Persist the record and return the API response
     */
    public AccountResponse create(CreateAccountCommand command) {

        log.info(
                "Execution step started: createAccount for customerId={}, accountType={}, currency={}",
                command.customerId(),
                command.accountType(),
                command.currency());

        // Step 1: Normalize the command into the domain value object.
        CustomerId customerId = CustomerId.of(command.customerId());

        // Step 2: Prevent duplicate account creation for the same customer and type.
        boolean duplicateCustomerAccount =
                accountRepository.existsByCustomerIdAndAccountType(
                        customerId,
                        command.accountType().name());
        log.info(
                "Duplicate check for customer/account type: customerId={}, accountType={}, exists={}",
                customerId.value(),
                command.accountType(),
                duplicateCustomerAccount);
        if (duplicateCustomerAccount) {
            throw new DuplicateAccountException(
                    "Account already exists for customer and account type");
        }

        // Step 3: Generate unique values for the database identity and account number.
        Long accountIdValue = accountIdGenerator.nextAccountId();
        String accountNumberValue = accountNumberGenerator.nextAccountNumber();
        log.info(
                "Generated new account identifiers: accountId={}, generatedAccountNumber={}",
                accountIdValue,
                accountNumberValue);

        AccountId accountId = AccountId.of(accountIdValue);
        AccountNumber accountNumber = AccountNumber.of(accountNumberValue);
        Currency currency = Currency.of(command.currency());

        // Step 4: Enforce the uniqueness of the generated account number.
        boolean duplicateAccountNumber = accountRepository.existsByAccountNumber(accountNumber);
        log.info(
                "Duplicate account-number check: accountNumber={}, exists={}",
                accountNumber.value(),
                duplicateAccountNumber);
        if (duplicateAccountNumber) {
            throw new DuplicateAccountException(
                    "Account number already exists: " + accountNumber.value());
        }

        // Step 5: Build the aggregate and let the domain model validate invariants.
        Account account = Account.create(
                accountId,
                accountNumber,
                customerId,
                command.accountType(),
                currency);

        // Step 6: Persist the aggregate and capture the stored state.
        Account savedAccount = accountRepository.save(account);
        log.info(
                "Execution step completed: createAccount for customerId={}, accountId={}, accountNumber={}",
                customerId.value(),
                savedAccount.accountId().value(),
                savedAccount.accountNumber().value());

        return toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(Long accountId) {

        log.info("Execution step started: getById for accountId={}", accountId);

        // Step 1: Load the account by its identifier.
        Account account = accountRepository
                .findById(AccountId.of(accountId))
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountId));

        log.info(
                "Execution step completed: getById for accountId={}, customerId={}",
                accountId,
                account.customerId().value());

        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByAccountNumber(String accountNumber) {

        log.info(
                "Execution step started: getByAccountNumber for accountNumber={}",
                accountNumber);

        // Step 1: Retrieve the account using the business key (account number).
        Account account = accountRepository
                .findByAccountNumber(AccountNumber.of(accountNumber))
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountNumber));

        log.info(
                "Execution step completed: getByAccountNumber for accountNumber={}, accountId={}",
                accountNumber,
                account.accountId().value());

        return toResponse(account);
    }

    /**
     * Lifecycle transition flow:
     * 1. Fetch current account state
     * 2. Apply the domain transition method
     * 3. Persist the updated aggregate
     */
    public AccountResponse activate(Long accountId) {

        log.info("Execution step started: activate for accountId={}", accountId);

        Account account = getAccount(accountId);
        account.activate();

        Account updatedAccount = accountRepository.save(account);
        log.info(
                "Execution step completed: activate for accountId={}, status={}",
                accountId,
                updatedAccount.status());

        return toResponse(updatedAccount);
    }

    public AccountResponse freeze(Long accountId) {

        log.info("Execution step started: freeze for accountId={}", accountId);

        Account account = getAccount(accountId);
        account.freeze();

        Account updatedAccount = accountRepository.save(account);
        log.info(
                "Execution step completed: freeze for accountId={}, status={}",
                accountId,
                updatedAccount.status());

        return toResponse(updatedAccount);
    }

    public AccountResponse unfreeze(Long accountId) {

        log.info("Execution step started: unfreeze for accountId={}", accountId);

        Account account = getAccount(accountId);
        account.unfreeze();

        Account updatedAccount = accountRepository.save(account);
        log.info(
                "Execution step completed: unfreeze for accountId={}, status={}",
                accountId,
                updatedAccount.status());

        return toResponse(updatedAccount);
    }

    public AccountResponse makeDormant(Long accountId) {

        log.info("Execution step started: makeDormant for accountId={}", accountId);

        Account account = getAccount(accountId);
        account.makeDormant();

        Account updatedAccount = accountRepository.save(account);
        log.info(
                "Execution step completed: makeDormant for accountId={}, status={}",
                accountId,
                updatedAccount.status());

        return toResponse(updatedAccount);
    }

    public AccountResponse close(Long accountId) {

        log.info("Execution step started: close for accountId={}", accountId);

        Account account = getAccount(accountId);
        account.close();

        Account updatedAccount = accountRepository.save(account);
        log.info(
                "Execution step completed: close for accountId={}, status={}",
                accountId,
                updatedAccount.status());

        return toResponse(updatedAccount);
    }

    private Account getAccount(Long accountId) {

        log.info("Execution step: getAccount for accountId={}", accountId);

        // Retrieve the current aggregate before applying any lifecycle transition.
        return accountRepository
                .findById(AccountId.of(accountId))
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountId));
    }

    private AccountResponse toResponse(Account account) {

        // Convert the aggregate to the outward-facing response model.
        return new AccountResponse(
                account.accountId().value(),
                account.customerId().value(),
                account.accountNumber().value(),
                account.accountType(),
                account.status(),
                account.currency().code(),
                account.createdAt(),
                account.updatedAt());
    }
}
