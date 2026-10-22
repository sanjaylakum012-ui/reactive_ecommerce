package org.demo.reactive_ecommerce.Repositories;

import org.demo.reactive_ecommerce.Entity.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

}
