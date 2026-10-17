package com.example.orders.web;

import com.example.orders.domain.IllegalOrderTransitionException;
import com.example.orders.domain.InvalidOrderException;
import com.example.orders.domain.OrderNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Turns errors into RFC 9457 problem details, as the contract requires for all
 * 4xx responses. Spring MVC's own errors (malformed JSON, bean validation, type
 * mismatches, unsupported media types) are handled by the superclass.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_BASE = "https://example.com/problems/";

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "order-not-found", "Order not found", e.getMessage());
    }

    @ExceptionHandler(IllegalOrderTransitionException.class)
    ProblemDetail illegalTransition(IllegalOrderTransitionException e) {
        return problem(HttpStatus.CONFLICT, "illegal-transition", "Status change not allowed", e.getMessage());
    }

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail invalidOrder(InvalidOrderException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-order", "Invalid order", e.getMessage());
    }

    /** Parameter constraints on the generated @Validated interface (e.g. limit 1..100). */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(ConstraintViolationException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-parameter", "Invalid request parameter", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + type));
        problem.setTitle(title);
        return problem;
    }
}
