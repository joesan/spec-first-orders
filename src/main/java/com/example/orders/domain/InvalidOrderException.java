package com.example.orders.domain;

/** The order violates rule R1. Mapped to HTTP 400. */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
