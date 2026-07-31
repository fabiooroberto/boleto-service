package com.santander.mock.controller;

import com.santander.mock.model.BankSlipListResponse;
import com.santander.mock.model.BoletoResponse;
import com.santander.mock.service.BoletoMockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/collection_bill_management/v2/bills")
public class BillsController {

    private final BoletoMockService service;

    public BillsController(BoletoMockService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<BankSlipListResponse<BoletoResponse>> listBillsByNumber(
            @RequestParam(value = "bankNumber") String bankNumber,
            @RequestParam(value = "beneficiaryCode") String beneficiaryCode) {

        // Validate bankNumber format
        if (bankNumber == null || bankNumber.isEmpty() || bankNumber.length() > 13 ||
            !bankNumber.matches("^[0-9]{1,13}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "bankNumber must be 1-13 numeric characters");
        }

        // Validate beneficiaryCode format
        if (beneficiaryCode == null || beneficiaryCode.isEmpty() || beneficiaryCode.length() > 9 ||
            !beneficiaryCode.matches("^[0-9]{1,9}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "beneficiaryCode must be 1-9 numeric characters");
        }

        // Search by covenantCode (beneficiaryCode) and bankNumber
        BoletoResponse boleto = service.getBoletoByBankNumberAndCovenantCode(bankNumber, beneficiaryCode);

        // Wrap in pageable response
        BankSlipListResponse<BoletoResponse> response = new BankSlipListResponse<>(
            null, // pageable info
            java.util.List.of(boleto)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bill_id}")
    public ResponseEntity<BankSlipListResponse<BoletoResponse>> getBillById(
            @PathVariable("bill_id") String billId,
            @RequestParam(value = "tipoConsulta", defaultValue = "default") String tipoConsulta) {

        // Validate tipoConsulta
        if (!tipoConsulta.matches("^(default|bankslip|settlement|registry|duplicate)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "tipoConsulta must be one of: default, bankslip, settlement, registry, duplicate");
        }

        // Validate bill_id format (covenantCode.bankNumber)
        String[] parts = billId.split("\\.");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "bill_id must be in format: covenantCode.bankNumber");
        }

        String covenantCode = parts[0];
        String bankNumber = parts[1];

        // Get boleto by ID
        BoletoResponse boleto = service.getBoletoByBankNumberAndCovenantCode(bankNumber, covenantCode);

        // Wrap in pageable response
        BankSlipListResponse<BoletoResponse> response = new BankSlipListResponse<>(
            null, // pageable info
            java.util.List.of(boleto)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bill_id}/bank_slips")
    public ResponseEntity<Map<String, String>> generateBankSlipPdf(
            @PathVariable("bill_id") String billId,
            @RequestBody CreateBankSlipPdfRequest request) {

        // Validate payerDocumentNumber
        if (request.payerDocumentNumber() == null || request.payerDocumentNumber() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "payerDocumentNumber is required and must be positive");
        }

        // Verify boleto exists
        service.getBoletoById(billId);

        // Generate PDF link
        String pdfUrl = "https://mock-boleto-pdfs.santander.local/boletos/" + billId + "/boleto.pdf";

        Map<String, String> response = new HashMap<>();
        response.put("link", pdfUrl);

        return ResponseEntity.ok(response);
    }

    public record CreateBankSlipPdfRequest(Long payerDocumentNumber) {}
}
