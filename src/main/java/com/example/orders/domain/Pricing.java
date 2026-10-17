package com.example.orders.domain;

import java.util.List;

/** Pricing rules R2 and R3. */
public final class Pricing {

    /** R3: subtotal from which the volume discount applies (€1,000). */
    public static final long DISCOUNT_THRESHOLD_CENTS = 100_000;
    /** R3: discount rate in percent. */
    public static final long DISCOUNT_PERCENT = 10;
    /** R3: maximum discount (€500). */
    public static final long DISCOUNT_CAP_CENTS = 50_000;

    private Pricing() {
    }

    /** R2: sum of quantity × unit price over all lines. */
    public static long subtotalCents(List<OrderLine> lines) {
        long subtotal = 0;
        for (OrderLine line : lines) {
            subtotal = Math.addExact(subtotal, line.amountCents());
        }
        return subtotal;
    }

    /** R3: 10 % of the subtotal (rounded down) from €1,000, capped at €500; otherwise 0. */
    public static long discountCents(long subtotalCents) {
        if (subtotalCents < DISCOUNT_THRESHOLD_CENTS) {
            return 0;
        }
        long tenPercent = subtotalCents * DISCOUNT_PERCENT / 100;
        return Math.min(tenPercent, DISCOUNT_CAP_CENTS);
    }
}
