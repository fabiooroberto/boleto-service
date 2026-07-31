package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Payment(
    Double paidValue,
    Double interestValue,
    Double fineValue,
    @JsonProperty("deductionValue") String deductionValue,
    Double rebateValue,
    Double iofValue,
    String date,
    String type,
    Integer bankCode,
    String channel,
    String kind,
    @JsonProperty("txId") String txId
) {}
