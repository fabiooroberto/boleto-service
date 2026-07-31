package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DictKey(
    String type,
    @JsonProperty("dictKey")
    String dictKey
) {}
