package com.example.orders.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Storage port. The domain defines it; infrastructure implements it. */
public interface OrderRepository {

    void add(Order order);

    Optional<Order> findById(UUID id);

    /** Newest first, optionally filtered by status, at most {@code limit} orders. */
    List<Order> findRecent(OrderStatus status, int limit);

    /**
     * Atomically replaces the order with {@code change.apply(current)}. If
     * {@code change} throws, the stored order stays unchanged and the exception
     * propagates.
     *
     * @throws OrderNotFoundException if no order has this id
     */
    Order update(UUID id, UnaryOperator<Order> change);
}
