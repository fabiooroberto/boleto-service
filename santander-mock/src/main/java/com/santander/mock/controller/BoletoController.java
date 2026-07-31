package com.santander.mock.controller;

import com.santander.mock.model.*;
import com.santander.mock.service.BoletoMockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@RestController
@RequestMapping("/collection_bill_management/v2/workspaces/{workspace_id}/bank_slips")
public class BoletoController {

    private static final Set<String> VALID_STATUSES =
        Set.of("ATIVO", "BAIXADO", "LIQUIDADO", "LIQUIDADO PARCIALMENTE");

    private final BoletoMockService service;

    public BoletoController(BoletoMockService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BoletoResponse> createBoleto(
            @PathVariable("workspace_id") String workspaceId,
            @jakarta.validation.Valid @RequestBody BoletoRequest request) {
        BoletoResponse response = service.createBoleto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<UpdateBoletoResponse> patchBoleto(
            @PathVariable("workspace_id") String workspaceId,
            @jakarta.validation.Valid @RequestBody BoletoRequest request) {

        BoletoResponse response = service.getBoletoByIdentifiers(
            request.environment(), request.nsuCode(), request.nsuDate(),
            request.covenantCode(), request.bankNumber());

        BoletoResponse updatedResponse = service.updateBoleto(response.id(), request);

        UpdateBoletoResponse updateResponse = new UpdateBoletoResponse(
            response.covenantCode(),
            response.bankNumber(),
            "Alteração realizada com sucesso"
        );

        return ResponseEntity.status(HttpStatus.OK).body(updateResponse);
    }

    @GetMapping
    public ResponseEntity<BankSlipListResponse<BoletoResponse>> listBoletos(
            @PathVariable("workspace_id") String workspaceId,
            @RequestParam(value = "_limit", defaultValue = "10") int limit,
            @RequestParam(value = "_offset", defaultValue = "0") int offset,
            @RequestParam(value = "bankNumber", required = false) String bankNumber,
            @RequestParam(value = "clientNumber", required = false) String clientNumber,
            @RequestParam(value = "dueDateInitial", required = false) String dueDateInitial,
            @RequestParam(value = "dueDateFinal", required = false) String dueDateFinal,
            @RequestParam(value = "paymentDateInitial", required = false) String paymentDateInitial,
            @RequestParam(value = "paymentDateFinal", required = false) String paymentDateFinal,
            @RequestParam(value = "status") String status) {

        if (limit < 1 || limit > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "_limit must be between 1 and 50");
        }
        if (offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "_offset must be >= 0");
        }
        if (!VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be one of " + VALID_STATUSES);
        }

        BankSlipListQuery filter = new BankSlipListQuery(
            limit, offset, bankNumber, clientNumber,
            dueDateInitial, dueDateFinal, paymentDateInitial, paymentDateFinal, status);

        return ResponseEntity.ok(service.listBoletos(filter));
    }

    @GetMapping("/{bank_slip_id}")
    public ResponseEntity<BoletoResponse> getBoletoById(
            @PathVariable("workspace_id") String workspaceId,
            @PathVariable("bank_slip_id") String bankSlipId) {
        BoletoResponse response = service.getBoletoById(bankSlipId);
        return ResponseEntity.ok(response);
    }
}
