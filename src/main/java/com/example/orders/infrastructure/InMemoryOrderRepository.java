package com.example.orders.infrastructure;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderNotFoundException;
import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * In-memory storage for the reference service. Insertion order is kept so
 * "newest first" is well defined even when timestamps collide. Replace with a
 * real database adapter behind {@link OrderRepository}.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<UUID, Order> orders = new LinkedHashMap<>();

    @Override
    public synchronized void add(Order order) {
        if (orders.putIfAbsent(order.id(), order) != null) {
            throw new IllegalStateException("duplicate order id " + order.id());
        }
    }

    @Override
    public synchronized Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public synchronized List<Order> findRecent(OrderStatus status, int limit) {
        List<Order> all = new ArrayList<>(orders.values());
        List<Order> result = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && result.size() < limit; i--) {
            Order order = all.get(i);
            if (status == null || order.status() == status) {
                result.add(order);
            }
        }
        return result;
    }

    @Override
    public synchronized Order update(UUID id, UnaryOperator<Order> change) {
        Order current = orders.get(id);
        if (current == null) {
            throw new OrderNotFoundException(id);
        }
        Order updated = change.apply(current);
        if (!updated.id().equals(id)) {
            throw new IllegalStateException("an update must not change the order id");
        }
        orders.put(id, updated);
        return updated;
    }
}
