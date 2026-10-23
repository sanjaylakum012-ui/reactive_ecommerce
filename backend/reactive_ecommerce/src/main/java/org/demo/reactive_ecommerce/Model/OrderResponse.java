package org.demo.reactive_ecommerce.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record OrderResponse(
            Long id,
            Integer quantity,

            @JsonProperty("total_price")
            BigDecimal totalPrice,

            ProductDetails product
) {
    public record ProductDetails(
            Long id,
            String name
    ) {}
}