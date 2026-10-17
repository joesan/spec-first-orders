package com.example.orders.domain;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Use cases for feature 001. Plain Java: no Spring or web types. */
public class OrderService {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    private final OrderRepository repository;
    private final Clock clock;
    private final Supplier<UUID> ids;

    public OrderService(OrderRepository repository, Clock clock, Supplier<UUID> ids) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.ids = Objects.requireNonNull(ids);
    }

    /** S1, R1–R3. */
    public Order create(String customerId, List<OrderLine> lines) {
        Order order = Order.create(ids.get(), customerId, lines, clock.instant());
        repository.add(order);
        return order;
    }

    /** S4. */
    public Order get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** S4. {@code status} may be null (no filter); {@code limit} is clamped to 1..100. */
    public List<Order> list(OrderStatus status, Integer limit) {
        int effective = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        return repository.findRecent(status, effective);
    }

    /** S3, R4. */
    public Order pay(UUID id) {
        return repository.update(id, order -> order.transitionTo(OrderStatus.PAID));
    }

    /** S3, R4, R5. */
    public Order ship(UUID id) {
        return repository.update(id, order -> order.transitionTo(OrderStatus.SHIPPED));
    }

    /** S3, R4. */
    public Order cancel(UUID id) {
        return repository.update(id, order -> order.transitionTo(OrderStatus.CANCELLED));
    }
}
