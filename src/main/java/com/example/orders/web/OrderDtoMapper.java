package com.example.orders.web;

import com.example.orders.api.model.CreateOrderRequestDto;
import com.example.orders.api.model.OrderDto;
import com.example.orders.api.model.OrderLineDto;
import com.example.orders.api.model.OrderStatusDto;
import com.example.orders.domain.InvalidOrderException;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderLine;
import com.example.orders.domain.OrderStatus;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Maps between generated DTOs (the contract) and domain types. Web layer only. */
final class OrderDtoMapper {

    private OrderDtoMapper() {
    }

    static List<OrderLine> toDomainLines(CreateOrderRequestDto request) {
        List<OrderLine> lines = new ArrayList<>();
        for (OrderLineDto dto : request.getLines()) {
            if (dto == null) {
                throw new InvalidOrderException("order lines must not be null");
            }
            lines.add(new OrderLine(dto.getSku(), dto.getQuantity(), dto.getUnitPriceCents()));
        }
        return lines;
    }

    static OrderStatus toDomain(OrderStatusDto status) {
        return status == null ? null : OrderStatus.valueOf(status.getValue());
    }

    static OrderDto toDto(Order order) {
        return new OrderDto()
                .id(order.id())
                .customerId(order.customerId())
                .status(OrderStatusDto.fromValue(order.status().name()))
                .lines(order.lines().stream().map(OrderDtoMapper::toDto).toList())
                .subtotalCents(order.subtotalCents())
                .discountCents(order.discountCents())
                .totalCents(order.totalCents())
                .createdAt(order.createdAt().atOffset(ZoneOffset.UTC));
    }

    private static OrderLineDto toDto(OrderLine line) {
        return new OrderLineDto()
                .sku(line.sku())
                .quantity(line.quantity())
                .unitPriceCents(line.unitPriceCents());
    }
}
