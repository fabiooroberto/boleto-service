package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Discount(
    String type,
    DiscountDetail discountOne,
    DiscountDetail discountTwo,
    DiscountDetail discountThree
) {
    public record DiscountDetail(
        String value,
        @JsonProperty("limitDate")
        String limitDate
    ) {}
}
