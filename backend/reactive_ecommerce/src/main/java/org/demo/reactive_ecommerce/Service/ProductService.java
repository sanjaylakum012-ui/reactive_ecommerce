package org.demo.reactive_ecommerce.Service;

import org.demo.reactive_ecommerce.Entity.Order;
import org.demo.reactive_ecommerce.Entity.Product;
import org.demo.reactive_ecommerce.Model.CartFakeStoreResponse;
import org.demo.reactive_ecommerce.Model.ProductDetailsResponse;
import org.demo.reactive_ecommerce.Model.ProductFakeStoreResponse;
import org.demo.reactive_ecommerce.Repositories.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository productRepository, WebClient webClient) {
        this.productRepository = productRepository;
        this.webClient = webClient;
    }

    public Flux<Product> getAllProducts() {
        AtomicInteger attempt = new AtomicInteger(0);
        return Flux.defer(() -> {
                    int timeoutSeconds = 1 + attempt.getAndIncrement();
                    return productRepository.findAll().timeout(Duration.ofSeconds(timeoutSeconds));
                })
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                                .maxBackoff(Duration.ofSeconds(1))
                                .filter(this::isTransientFailure)
                                .doBeforeRetry(signal ->
                                        log.warn("Retry attempt: {}", signal.totalRetries() + 1)
                                )
                )
                .onErrorResume(error -> {
                    log.error("Unable to load products", error);
                    return Flux.error(new RuntimeException("Products are temporarily unavailable", error));
                });
    };

    public Mono<ProductDetailsResponse> getProductAndCartDetails() {
        Mono<List<ProductFakeStoreResponse>> productListMono = webClient.get()
                .uri("/products")
                .retrieve()
                .bodyToFlux(ProductFakeStoreResponse.class)
                .collectList()
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                );

        Mono<List<CartFakeStoreResponse>> cartMono = webClient.get()
                .uri("/carts")
                .retrieve()
                .bodyToFlux(CartFakeStoreResponse.class)
                .collectList()
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                );

        return Mono.zip(productListMono, cartMono)
                .map(tuple -> new ProductDetailsResponse(
                        tuple.getT1(),
                        tuple.getT2()
                ));
    }

    public Flux<Product> getProducts(){
        return productRepository.findAll().timeout(Duration.ofSeconds(2)).retryWhen(
                Retry.backoff(2, Duration.ofMillis(200)).filter(err -> err instanceof TimeoutException)
        );
    }

    private boolean isTransientFailure(Throwable throwable) {
        return throwable instanceof java.util.concurrent.TimeoutException;
    }
}
