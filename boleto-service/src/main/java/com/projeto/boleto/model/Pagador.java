package com.projeto.boleto.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Pagador(
    String name,
    @JsonProperty("documentType")
    String documentType,
    @JsonProperty("documentNumber")
    String documentNumber,
    String address,
    String neighborhood,
    String city,
    String state,
    @JsonProperty("zipCode")
    String zipCode
) {}
