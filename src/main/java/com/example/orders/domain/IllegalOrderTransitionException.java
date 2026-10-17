package com.example.orders.domain;

import java.util.UUID;

/** The requested status change is not allowed by rule R4. Mapped to HTTP 409. */
public class IllegalOrderTransitionException extends RuntimeException {

    private final OrderStatus from;
    private final OrderStatus to;

    public IllegalOrderTransitionException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("order %s cannot change from %s to %s".formatted(orderId, from, to));
        this.from = from;
        this.to = to;
    }

    public OrderStatus from() {
        return from;
    }

    public OrderStatus to() {
        return to;
    }
}
