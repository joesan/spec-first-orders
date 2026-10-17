package com.example.orders.domain;

import com.example.orders.infrastructure.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Example-based tests for scenario S4 (reading orders) and error paths. */
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrderService service =
            new OrderService(new InMemoryOrderRepository(), Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);

    @Test
    void createStoresOrderInStatusCreatedWithTimestamp() {
        Order order = create();

        assertThat(service.get(order.id())).isEqualTo(order);
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.customerId()).isEqualTo("cust-1");
    }

    @Test
    void getUnknownOrderThrowsNotFound() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void transitionsOnUnknownOrderThrowNotFound() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> service.pay(unknown)).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> service.ship(unknown)).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> service.cancel(unknown)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void listReturnsNewestFirst() {
        Order first = create();
        Order second = create();
        Order third = create();

        assertThat(service.list(null, null)).extracting(Order::id)
                .containsExactly(third.id(), second.id(), first.id());
    }

    @Test
    void listFiltersByStatus() {
        Order created = create();
        Order paid = create();
        service.pay(paid.id());

        assertThat(service.list(OrderStatus.PAID, null)).extracting(Order::id).containsExactly(paid.id());
        assertThat(service.list(OrderStatus.CREATED, null)).extracting(Order::id).containsExactly(created.id());
        assertThat(service.list(OrderStatus.SHIPPED, null)).isEmpty();
    }

    @Test
    void listHonoursLimitAndDefaultsTo20() {
        for (int i = 0; i < 25; i++) {
            create();
        }
        assertThat(service.list(null, null)).hasSize(OrderService.DEFAULT_LIMIT);
        assertThat(service.list(null, 3)).hasSize(3);
        assertThat(service.list(null, 1)).hasSize(1);
    }

    @Test
    void listClampsLimitToAllowedRange() {
        for (int i = 0; i < 3; i++) {
            create();
        }
        assertThat(service.list(null, 0)).hasSize(1);
        assertThat(service.list(null, 1_000)).hasSize(3);
    }

    @Test
    void blankCustomerIdIsRejected() {
        List<OrderLine> lines = List.of(new OrderLine("SKU-1", 1, 1));
        assertThatThrownBy(() -> service.create(" ", lines)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> service.create(null, lines)).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void blankSkuIsRejected() {
        assertThatThrownBy(() -> new OrderLine(" ", 1, 1)).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void rejectedTransitionReportsFromAndTo() {
        Order order = create();
        service.cancel(order.id());

        assertThatThrownBy(() -> service.ship(order.id()))
                .isInstanceOfSatisfying(IllegalOrderTransitionException.class, e -> {
                    assertThat(e.from()).isEqualTo(OrderStatus.CANCELLED);
                    assertThat(e.to()).isEqualTo(OrderStatus.SHIPPED);
                });
    }

    private Order create() {
        return service.create("cust-1", List.of(new OrderLine("SKU-1", 2, 150_000)));
    }
}
