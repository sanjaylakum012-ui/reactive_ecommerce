package org.demo.reactive_ecommerce.Controller;

import org.demo.reactive_ecommerce.Entity.Product;
import org.demo.reactive_ecommerce.Model.ProductDetailsResponse;
import org.demo.reactive_ecommerce.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public Flux<Product> getAllProducts(){
        return productService.getAllProducts();
    }

    @GetMapping("/cart")
    public Mono<ProductDetailsResponse> getCartAndProduct(){
        return productService.getProductAndCartDetails();
    }
}
