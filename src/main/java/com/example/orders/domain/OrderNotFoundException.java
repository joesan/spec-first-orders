package com.example.orders.domain;

import java.util.UUID;

/** No order exists with the given id. Mapped to HTTP 404. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("order %s not found".formatted(orderId));
    }
}
