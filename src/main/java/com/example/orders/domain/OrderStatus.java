package com.example.orders.domain;

import java.util.Set;

/** Order lifecycle (rule R4). */
public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    CANCELLED;

    /** R4: the only transitions allowed out of each status. */
    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case CREATED -> Set.of(PAID, CANCELLED);
            case PAID -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
