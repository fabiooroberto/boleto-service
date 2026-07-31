package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record Beneficiary(
    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "documentType is required")
    @JsonProperty("documentType")
    String documentType,

    @NotBlank(message = "documentNumber is required")
    @JsonProperty("documentNumber")
    String documentNumber
) {}
