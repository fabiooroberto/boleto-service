package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PageableInfo(
    @JsonProperty("_limit") int limit,
    @JsonProperty("_offset") int offset,
    @JsonProperty("_pageNumber") int pageNumber,
    @JsonProperty("_pageElements") int pageElements,
    @JsonProperty("_totalPages") int totalPages,
    @JsonProperty("_totalElements") long totalElements
) {}
