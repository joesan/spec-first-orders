package com.example.orders.domain;

import java.util.Objects;

/**
 * One ordered item. Enforces the per-line limits of rule R1, independently of
 * the request validation in the web layer.
 */
public record OrderLine(String sku, int quantity, long unitPriceCents) {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 1_000;
    public static final long MIN_UNIT_PRICE_CENTS = 1;
    public static final long MAX_UNIT_PRICE_CENTS = 10_000_000;

    public OrderLine {
        Objects.requireNonNull(sku, "sku");
        if (sku.isBlank()) {
            throw new InvalidOrderException("sku must not be blank");
        }
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new InvalidOrderException(
                    "quantity must be between %d and %d".formatted(MIN_QUANTITY, MAX_QUANTITY));
        }
        if (unitPriceCents < MIN_UNIT_PRICE_CENTS || unitPriceCents > MAX_UNIT_PRICE_CENTS) {
            throw new InvalidOrderException(
                    "unitPriceCents must be between %d and %d".formatted(MIN_UNIT_PRICE_CENTS, MAX_UNIT_PRICE_CENTS));
        }
    }

    /** Line amount. Cannot overflow: at most 1,000 × 10,000,000. */
    public long amountCents() {
        return quantity * unitPriceCents;
    }
}
