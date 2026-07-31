package com.projeto.boleto.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PageableInfo(
    @JsonProperty("_limit")
    int limit,
    @JsonProperty("_offset")
    int offset,
    @JsonProperty("_pageNumber")
    int page,
    @JsonProperty("_pageElements")
    int size,
    @JsonProperty("_totalPages")
    int totalPages,
    @JsonProperty("_totalElements")
    long totalElements
) {}
