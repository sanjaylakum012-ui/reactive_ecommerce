package org.demo.reactive_ecommerce.Controller;

import org.demo.reactive_ecommerce.Model.OrderResponse;
import org.demo.reactive_ecommerce.Service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/")
    public Flux<OrderResponse> getOrders(){
        return orderService.getOrdersWithProduct();
    }
}
