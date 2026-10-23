package org.demo.reactive_ecommerce.Model;

import java.time.Instant;
import java.util.List;

public record CartFakeStoreResponse(
        Integer id,
        Integer userId,
        Instant date,
        List<CartProduct> products
) {
    public record CartProduct(
            Integer productId,
            Integer quantity
    ) {}
}
