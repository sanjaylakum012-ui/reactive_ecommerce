package org.demo.reactive_ecommerce.Model;

public record ProductFakeStoreResponse(
        Integer id,
        String title,
        Double price,
        String description,
        String category,
        String image
) { }
