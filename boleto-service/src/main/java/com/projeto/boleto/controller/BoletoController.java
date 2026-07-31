package com.projeto.boleto.controller;

import com.projeto.boleto.mapper.BoletoMapper;
import com.projeto.boleto.model.BankSlipListQuery;
import com.projeto.boleto.model.BankSlipListResponse;
import com.projeto.boleto.model.BoletoRequest;
import com.projeto.boleto.model.BoletoResponse;
import com.projeto.boleto.model.UpdateBoletoResponse;
import com.projeto.boleto.service.BoletoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/v1/bank_slips")
public class BoletoController {

    @Value("${santander.api.workspace-id}")
    private String workspaceId;

    @Value("${santander.api.environment}")
    private String environment;

    private final BoletoService boletoService;
    private final BoletoMapper boletoMapper;

    public BoletoController(BoletoService boletoService, BoletoMapper boletoMapper) {
        this.boletoService = boletoService;
        this.boletoMapper = boletoMapper;
    }

    @PostMapping
    public ResponseEntity<BoletoResponse> createBoleto(
            @jakarta.validation.Valid @RequestBody BoletoRequest request) {
        BoletoRequest withEnvironment = boletoMapper.withEnvironment(request, environment);
        BoletoResponse response = boletoService.createBoleto(withEnvironment);
        URI location = URI.create("/v1/bank_slips/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<?> listBoletos(
            @RequestParam(value = "_limit", defaultValue = "10") int limit,
            @RequestParam(value = "_offset", defaultValue = "0") int offset,
            @RequestParam(value = "bankNumber", required = false) String bankNumber,
            @RequestParam(value = "clientNumber", required = false) String clientNumber,
            @RequestParam(value = "dueDateInitial", required = false) String dueDateInitial,
            @RequestParam(value = "dueDateFinal", required = false) String dueDateFinal,
            @RequestParam(value = "paymentDateInitial", required = false) String paymentDateInitial,
            @RequestParam(value = "paymentDateFinal", required = false) String paymentDateFinal,
            @RequestParam(value = "status", defaultValue = "ATIVO") String status) {
        BankSlipListQuery query = new BankSlipListQuery(
            limit, offset, bankNumber, clientNumber,
            dueDateInitial, dueDateFinal, paymentDateInitial, paymentDateFinal, status);
        BankSlipListResponse response = boletoService.listBoletos(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bank_slip_id}")
    public ResponseEntity<BoletoResponse> getBoletoById(
            @PathVariable("bank_slip_id") String bankSlipId) {
        BoletoResponse response = boletoService.getBoletoById(bankSlipId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<UpdateBoletoResponse> patchBoleto(
            @jakarta.validation.Valid @RequestBody BoletoRequest request) {
        BoletoRequest withEnvironment = boletoMapper.withEnvironment(request, environment);
        boletoService.updateBoleto(withEnvironment);
        UpdateBoletoResponse updateResponse = new UpdateBoletoResponse(
            withEnvironment.covenantCode(),
            withEnvironment.bankNumber(),
            "Alteração realizada com sucesso"
        );
        return ResponseEntity.status(HttpStatus.OK).body(updateResponse);
    }

}
