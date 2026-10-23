package org.demo.reactive_ecommerce.Repositories;

import org.demo.reactive_ecommerce.Entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
