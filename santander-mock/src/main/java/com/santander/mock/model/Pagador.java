package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Pagador(
    String nome,
    @JsonProperty("cpf")
    String cpfCnpj,
    @JsonProperty("endereco")
    String logradouro,
    @JsonProperty("numero")
    String numero,
    String bairro,
    String cidade,
    String estado,
    String cep
) {}
