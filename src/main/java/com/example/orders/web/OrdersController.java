package com.example.orders.web;

import com.example.orders.api.OrdersApi;
import com.example.orders.api.model.CreateOrderRequestDto;
import com.example.orders.api.model.OrderDto;
import com.example.orders.api.model.OrderPageDto;
import com.example.orders.api.model.OrderStatusDto;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Implements the interface generated from api/openapi.yaml. Request mappings,
 * parameter binding and validation annotations come from {@link OrdersApi};
 * do not add mapping annotations here.
 */
@RestController
public class OrdersController implements OrdersApi {

    private final OrderService orders;

    public OrdersController(OrderService orders) {
        this.orders = orders;
    }

    @Override
    public ResponseEntity<OrderDto> createOrder(CreateOrderRequestDto request) {
        Order order = orders.create(request.getCustomerId(), OrderDtoMapper.toDomainLines(request));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(PATH_GET_ORDER)
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(OrderDtoMapper.toDto(order));
    }

    @Override
    public ResponseEntity<OrderPageDto> listOrders(OrderStatusDto status, Integer limit) {
        var items = orders.list(OrderDtoMapper.toDomain(status), limit).stream()
                .map(OrderDtoMapper::toDto)
                .toList();
        return ResponseEntity.ok(new OrderPageDto().items(items));
    }

    @Override
    public ResponseEntity<OrderDto> getOrder(UUID orderId) {
        return ResponseEntity.ok(OrderDtoMapper.toDto(orders.get(orderId)));
    }

    @Override
    public ResponseEntity<OrderDto> payOrder(UUID orderId) {
        return ResponseEntity.ok(OrderDtoMapper.toDto(orders.pay(orderId)));
    }

    @Override
    public ResponseEntity<OrderDto> shipOrder(UUID orderId) {
        return ResponseEntity.ok(OrderDtoMapper.toDto(orders.ship(orderId)));
    }

    @Override
    public ResponseEntity<OrderDto> cancelOrder(UUID orderId) {
        return ResponseEntity.ok(OrderDtoMapper.toDto(orders.cancel(orderId)));
    }
}
