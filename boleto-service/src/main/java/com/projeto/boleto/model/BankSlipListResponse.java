package com.projeto.boleto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BankSlipListResponse<T>(
    @JsonProperty("_pageable")
    PageableInfo pageable,
    @JsonProperty("_content")
    List<T> data
) {}
