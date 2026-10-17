package com.example.orders;

import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderService;
import com.example.orders.infrastructure.InMemoryOrderRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.UUID;

@SpringBootApplication
public class OrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }

    @Bean
    OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    OrderService orderService(OrderRepository repository) {
        return new OrderService(repository, Clock.systemUTC(), UUID::randomUUID);
    }
}
