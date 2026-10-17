package com.example.orders.domain;

import com.example.orders.infrastructure.InMemoryOrderRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property tests for the business rules in specs/001-order-management/spec.md.
 *
 * These tests are part of the spec (see CODEOWNERS). Each property is derived
 * from the rule text and named after its rule id. They must not be weakened to
 * make an implementation pass.
 */
class OrderRulesProperties {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    // ---------------------------------------------------------------- R1

    @Property
    @Label("R1: any 1..50 valid lines form a valid order")
    void r1_validLineCountsAreAccepted(@ForAll("validLines") List<OrderLine> lines) {
        Order order = Order.create(UUID.randomUUID(), "cust-1", lines, NOW);
        assertThat(order.lines()).hasSize(lines.size());
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Property
    @Label("R1: more than 50 lines are rejected")
    void r1_tooManyLinesAreRejected(@ForAll @IntRange(min = 51, max = 200) int count) {
        List<OrderLine> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(new OrderLine("SKU-" + i, 1, 1));
        }
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), "cust-1", lines, NOW))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Example
    @Label("R1: an order without lines is rejected")
    void r1_emptyOrderIsRejected() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), "cust-1", List.of(), NOW))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Property
    @Label("R1: quantity outside 1..1000 is rejected")
    void r1_quantityOutOfRangeIsRejected(@ForAll("quantitiesOutOfRange") int quantity) {
        assertThatThrownBy(() -> new OrderLine("SKU-1", quantity, 100))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Property
    @Label("R1: unit price outside 1..10,000,000 cents is rejected")
    void r1_priceOutOfRangeIsRejected(@ForAll("pricesOutOfRange") long unitPriceCents) {
        assertThatThrownBy(() -> new OrderLine("SKU-1", 1, unitPriceCents))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Property
    @Label("R1: boundary values are accepted")
    void r1_boundariesAreAccepted(
            @ForAll("boundaryQuantities") int quantity,
            @ForAll("boundaryPrices") long unitPriceCents) {
        OrderLine line = new OrderLine("SKU-1", quantity, unitPriceCents);
        assertThat(line.quantity()).isEqualTo(quantity);
        assertThat(line.unitPriceCents()).isEqualTo(unitPriceCents);
    }

    // ---------------------------------------------------------------- R2

    @Property
    @Label("R2: subtotal is the sum of quantity x unit price")
    void r2_subtotalIsSumOfLineAmounts(@ForAll("validLines") List<OrderLine> lines) {
        long expected = 0;
        for (OrderLine line : lines) {
            expected += (long) line.quantity() * line.unitPriceCents();
        }
        Order order = Order.create(UUID.randomUUID(), "cust-1", lines, NOW);
        assertThat(order.subtotalCents()).isEqualTo(expected);
    }

    @Property
    @Label("R2: total = subtotal - discount, and is never negative")
    void r2_totalIsSubtotalMinusDiscountAndNonNegative(@ForAll("validLines") List<OrderLine> lines) {
        Order order = Order.create(UUID.randomUUID(), "cust-1", lines, NOW);
        assertThat(order.totalCents()).isEqualTo(order.subtotalCents() - order.discountCents());
        assertThat(order.totalCents()).isNotNegative();
    }

    // ---------------------------------------------------------------- R3

    @Property
    @Label("R3: no discount below 100,000 cents")
    void r3_noDiscountBelowThreshold(@ForAll @LongRange(min = 1, max = 99_999) long subtotal) {
        assertThat(Pricing.discountCents(subtotal)).isZero();
    }

    @Property
    @Label("R3: from 100,000 cents the discount is 10 % rounded down, capped at 50,000")
    void r3_discountIsTenPercentCapped(@ForAll @LongRange(min = 100_000, max = 500_000_000_000L) long subtotal) {
        long expected = Math.min(subtotal / 10, 50_000);
        assertThat(Pricing.discountCents(subtotal)).isEqualTo(expected);
    }

    @Property
    @Label("R3: the discount never exceeds the cap or the subtotal")
    void r3_discountNeverExceedsCapOrSubtotal(@ForAll("validLines") List<OrderLine> lines) {
        Order order = Order.create(UUID.randomUUID(), "cust-1", lines, NOW);
        assertThat(order.discountCents())
                .isBetween(0L, 50_000L)
                .isLessThanOrEqualTo(order.subtotalCents());
    }

    @Example
    @Label("R3: exact threshold and cap boundaries")
    void r3_boundaries() {
        assertThat(Pricing.discountCents(99_999)).isZero();
        assertThat(Pricing.discountCents(100_000)).isEqualTo(10_000);
        assertThat(Pricing.discountCents(100_009)).isEqualTo(10_000);
        assertThat(Pricing.discountCents(499_999)).isEqualTo(49_999);
        assertThat(Pricing.discountCents(500_000)).isEqualTo(50_000);
        assertThat(Pricing.discountCents(500_010)).isEqualTo(50_000);
    }

    // ---------------------------------------------------------------- R4

    @Property
    @Label("R4: only the four listed transitions are allowed")
    void r4_onlyListedTransitionsAreAllowed(@ForAll OrderStatus from, @ForAll OrderStatus to) {
        boolean listed = (from == OrderStatus.CREATED && to == OrderStatus.PAID)
                || (from == OrderStatus.PAID && to == OrderStatus.SHIPPED)
                || (from == OrderStatus.CREATED && to == OrderStatus.CANCELLED)
                || (from == OrderStatus.PAID && to == OrderStatus.CANCELLED);
        assertThat(from.canTransitionTo(to)).isEqualTo(listed);
    }

    @Property
    @Label("R4: a rejected transition leaves the order unchanged")
    void r4_rejectedTransitionLeavesOrderUnchanged(@ForAll("actionSequences") List<OrderStatus> actions) {
        OrderService service = newService();
        Order order = service.create("cust-1", List.of(new OrderLine("SKU-1", 1, 100)));
        for (OrderStatus target : actions) {
            Order before = service.get(order.id());
            try {
                apply(service, order.id(), target);
                assertThat(before.status().canTransitionTo(target)).isTrue();
            } catch (IllegalOrderTransitionException e) {
                assertThat(before.status().canTransitionTo(target)).isFalse();
                assertThat(service.get(order.id())).isEqualTo(before);
            }
        }
    }

    @Property
    @Label("R4: SHIPPED and CANCELLED are final")
    void r4_finalStatesAreFinal(@ForAll OrderStatus status) {
        boolean shouldBeFinal = status == OrderStatus.SHIPPED || status == OrderStatus.CANCELLED;
        assertThat(status.isFinal()).isEqualTo(shouldBeFinal);
    }

    // ---------------------------------------------------------------- R5

    @Property
    @Label("R5: a cancelled order is never shipped, whatever happens before or after")
    void r5_cancelledOrderIsNeverShipped(@ForAll("actionSequences") List<OrderStatus> actions) {
        OrderService service = newService();
        Order order = service.create("cust-1", List.of(new OrderLine("SKU-1", 1, 100)));
        boolean wasCancelled = false;
        for (OrderStatus target : actions) {
            try {
                apply(service, order.id(), target);
            } catch (IllegalOrderTransitionException ignored) {
                // rejected transitions are allowed; R4 checks their effect
            }
            OrderStatus now = service.get(order.id()).status();
            wasCancelled |= now == OrderStatus.CANCELLED;
            if (wasCancelled) {
                assertThat(now).isNotEqualTo(OrderStatus.SHIPPED);
            }
        }
    }

    // ---------------------------------------------------------------- generators

    @Provide
    Arbitrary<List<OrderLine>> validLines() {
        Arbitrary<String> skus = Arbitraries.strings().withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-")
                .ofMinLength(3).ofMaxLength(32);
        Arbitrary<Integer> quantities = Arbitraries.integers().between(1, 1_000);
        Arbitrary<Long> prices = Arbitraries.longs().between(1, 10_000_000);
        return Combinators.combine(skus, quantities, prices).as(OrderLine::new)
                .list().ofMinSize(1).ofMaxSize(50);
    }

    @Provide
    Arbitrary<Integer> quantitiesOutOfRange() {
        return Arbitraries.oneOf(
                Arbitraries.integers().lessOrEqual(0),
                Arbitraries.integers().greaterOrEqual(1_001));
    }

    @Provide
    Arbitrary<Long> pricesOutOfRange() {
        return Arbitraries.oneOf(
                Arbitraries.longs().lessOrEqual(0),
                Arbitraries.longs().greaterOrEqual(10_000_001));
    }

    @Provide
    Arbitrary<Integer> boundaryQuantities() {
        return Arbitraries.of(1, 1_000);
    }

    @Provide
    Arbitrary<Long> boundaryPrices() {
        return Arbitraries.of(1L, 10_000_000L);
    }

    @Provide
    Arbitrary<List<OrderStatus>> actionSequences() {
        return Arbitraries.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.CANCELLED)
                .list().ofMinSize(1).ofMaxSize(8);
    }

    // ---------------------------------------------------------------- helpers

    private static OrderService newService() {
        return new OrderService(new InMemoryOrderRepository(), Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);
    }

    private static void apply(OrderService service, UUID id, OrderStatus target) {
        switch (target) {
            case PAID -> service.pay(id);
            case SHIPPED -> service.ship(id);
            case CANCELLED -> service.cancel(id);
            case CREATED -> throw new IllegalArgumentException("CREATED is not a target");
        }
    }
}
