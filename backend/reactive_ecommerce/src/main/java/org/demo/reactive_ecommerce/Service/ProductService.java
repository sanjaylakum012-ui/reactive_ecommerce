package org.demo.reactive_ecommerce.Service;

import lombok.extern.slf4j.Slf4j;
import org.demo.reactive_ecommerce.Entity.Product;
import org.demo.reactive_ecommerce.Repositories.ProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
                    return Flux.error(
                            new RuntimeException("Products are temporarily unavailable", error)
                    );
                });
    };

    private boolean isTransientFailure(Throwable throwable) {
        return throwable instanceof java.util.concurrent.TimeoutException;
    }
}
