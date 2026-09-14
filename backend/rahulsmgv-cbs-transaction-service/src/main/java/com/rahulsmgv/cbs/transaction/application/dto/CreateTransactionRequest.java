package com.rahulsmgv.cbs.transaction.application.dto;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(

        @NotBlank(message = "Transaction reference must not be blank")
        String transactionReference,

        @NotBlank(message = "Idempotency key must not be blank")
        String idempotencyKey,

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        UUID sourceAccountId,

        UUID destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,

        @NotBlank(message = "Currency must not be blank")
        String currency,

        String description
) {
}