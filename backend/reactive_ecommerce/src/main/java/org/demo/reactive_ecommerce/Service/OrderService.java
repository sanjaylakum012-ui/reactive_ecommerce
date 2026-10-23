package org.demo.reactive_ecommerce.Service;

import org.demo.reactive_ecommerce.Model.OrderResponse;
import org.demo.reactive_ecommerce.Repositories.OrderRepository;
import org.demo.reactive_ecommerce.Repositories.ProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public Flux<OrderResponse> getOrdersWithProduct() {
        return orderRepository.findAll()
                .flatMap(order -> productRepository.findById(order.getProductId()).map(product -> new OrderResponse(
                                        order.getId(),
                                        order.getQuantity(),
                                        order.getTotalPrice(),
                                        new OrderResponse.ProductDetails(product.getId(), product.getName()))));
    }
}
