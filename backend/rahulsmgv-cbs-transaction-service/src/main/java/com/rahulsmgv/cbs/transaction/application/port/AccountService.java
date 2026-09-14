package com.rahulsmgv.cbs.transaction.application.port;

import java.util.UUID;

public interface AccountService {

    boolean exists(UUID accountId);

    boolean isActive(UUID accountId);

    boolean hasSufficientBalance(
            UUID accountId,
            java.math.BigDecimal amount
    );
}