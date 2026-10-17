package com.example.orders.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An order. Immutable: state changes return a new instance. Totals are always
 * derived from the lines (R2, R3), never supplied by callers.
 */
public final class Order {

    public static final int MIN_LINES = 1;
    public static final int MAX_LINES = 50;

    private final UUID id;
    private final String customerId;
    private final OrderStatus status;
    private final List<OrderLine> lines;
    private final long subtotalCents;
    private final long discountCents;
    private final Instant createdAt;

    private Order(UUID id, String customerId, OrderStatus status, List<OrderLine> lines, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.status = Objects.requireNonNull(status, "status");
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.subtotalCents = Pricing.subtotalCents(this.lines);
        this.discountCents = Pricing.discountCents(this.subtotalCents);
    }

    /** Creates a new order in status CREATED, enforcing R1. */
    public static Order create(UUID id, String customerId, List<OrderLine> lines, Instant createdAt) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("customerId must not be blank");
        }
        if (lines == null || lines.size() < MIN_LINES || lines.size() > MAX_LINES) {
            throw new InvalidOrderException(
                    "an order needs between %d and %d lines".formatted(MIN_LINES, MAX_LINES));
        }
        for (OrderLine line : lines) {
            if (line == null) {
                throw new InvalidOrderException("order lines must not be null");
            }
        }
        return new Order(id, customerId, OrderStatus.CREATED, lines, createdAt);
    }

    /** R4/R5: returns this order in the target status, or throws if the transition is not allowed. */
    public Order transitionTo(OrderStatus target) {
        Objects.requireNonNull(target, "target");
        if (!status.canTransitionTo(target)) {
            throw new IllegalOrderTransitionException(id, status, target);
        }
        return new Order(id, customerId, target, lines, createdAt);
    }

    public UUID id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public long subtotalCents() {
        return subtotalCents;
    }

    public long discountCents() {
        return discountCents;
    }

    /** R2: subtotal minus discount. */
    public long totalCents() {
        return subtotalCents - discountCents;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Order other
                && id.equals(other.id)
                && customerId.equals(other.customerId)
                && status == other.status
                && lines.equals(other.lines)
                && createdAt.equals(other.createdAt));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, status, lines, createdAt);
    }

    @Override
    public String toString() {
        return "Order[id=%s, status=%s, lines=%d, totalCents=%d]".formatted(id, status, lines.size(), totalCents());
    }
}
