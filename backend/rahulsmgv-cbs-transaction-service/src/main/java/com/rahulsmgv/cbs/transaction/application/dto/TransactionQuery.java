package com.rahulsmgv.cbs.transaction.application.dto;

import com.rahulsmgv.cbs.transaction.domain.enums.TransactionStatus;
import com.rahulsmgv.cbs.transaction.domain.enums.TransactionType;

import java.util.UUID;

public record TransactionQuery(

        UUID accountId,

        TransactionStatus status,

        TransactionType transactionType
) {
}