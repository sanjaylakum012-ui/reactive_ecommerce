package org.demo.reactive_ecommerce.Model;

import java.util.List;

public record ProductDetailsResponse(
        List<ProductFakeStoreResponse> product,
        List<CartFakeStoreResponse> carts
) { }
