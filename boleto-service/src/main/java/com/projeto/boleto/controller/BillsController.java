package com.projeto.boleto.controller;

import com.projeto.boleto.model.BankSlipListResponse;
import com.projeto.boleto.model.BoletoResponse;
import com.projeto.boleto.service.BoletoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/bills")
public class BillsController {

    private final BoletoService service;

    public BillsController(BoletoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<BankSlipListResponse<BoletoResponse>> listBillsByNumber(
            @RequestParam(value = "bankNumber") String bankNumber,
            @RequestParam(value = "beneficiaryCode") String beneficiaryCode) {
        if (bankNumber == null || bankNumber.isEmpty() || !bankNumber.matches("^[0-9]{1,13}$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bankNumber");
        if (beneficiaryCode == null || beneficiaryCode.isEmpty() || !beneficiaryCode.matches("^[0-9]{1,9}$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid beneficiaryCode");
        BoletoResponse boleto = service.getBillByNumber(bankNumber, beneficiaryCode);
        return ResponseEntity.ok(new BankSlipListResponse<>(null, java.util.List.of(boleto)));
    }

    @GetMapping("/{bill_id}")
    public ResponseEntity<BankSlipListResponse<BoletoResponse>> getBillById(
            @PathVariable("bill_id") String billId,
            @RequestParam(value = "tipoConsulta", defaultValue = "default") String tipoConsulta) {
        if (!tipoConsulta.matches("^(default|bankslip|settlement|registry|duplicate)$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tipoConsulta");
        String[] parts = billId.split("\\.");
        if (parts.length != 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid format");
        BoletoResponse boleto = service.getBillByNumber(parts[1], parts[0]);
        return ResponseEntity.ok(new BankSlipListResponse<>(null, java.util.List.of(boleto)));
    }

    @PostMapping("/{bill_id}/bank_slips")
    public ResponseEntity<Map<String, String>> generateBankSlipPdf(
            @PathVariable("bill_id") String billId,
            @RequestBody CreateBankSlipPdfRequest request) {
        if (request.payerDocumentNumber() == null || request.payerDocumentNumber() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payerDocumentNumber");

        String pdfUrl = service.generatePdf(billId, request.payerDocumentNumber());
        Map<String, String> response = new HashMap<>();
        response.put("link", pdfUrl);
        return ResponseEntity.ok(response);
    }

    public record CreateBankSlipPdfRequest(Long payerDocumentNumber) {}
}
