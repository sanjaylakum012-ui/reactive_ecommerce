package com.rahulsmgv.cbs.transaction.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(
        BigDecimal amount,
        String currency
) {

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }

        currency = currency.toUpperCase();
    }
}