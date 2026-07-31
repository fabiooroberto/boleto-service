package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record Payer(
    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "documentType is required")
    @JsonProperty("documentType")
    String documentType,

    @NotBlank(message = "documentNumber is required")
    @JsonProperty("documentNumber")
    String documentNumber,

    @NotBlank(message = "address is required")
    String address,

    @NotBlank(message = "neighborhood is required")
    String neighborhood,

    @NotBlank(message = "city is required")
    String city,

    @NotBlank(message = "state is required")
    String state,

    @NotBlank(message = "zipCode is required")
    @JsonProperty("zipCode")
    String zipCode
) {}
