package com.projeto.boleto.service;

import com.projeto.boleto.client.SantanderClient;
import com.projeto.boleto.mapper.BoletoMapper;
import com.projeto.boleto.model.BankSlipListQuery;
import com.projeto.boleto.model.BankSlipListResponse;
import com.projeto.boleto.model.Boleto;
import com.projeto.boleto.model.BoletoRequest;
import com.projeto.boleto.model.BoletoResponse;
import com.projeto.boleto.repository.BoletoRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class BoletoService {

    private final SantanderClient santanderClient;
    private final BoletoRepository boletoRepository;
    private final BoletoMapper boletoMapper;

    public BoletoService(SantanderClient santanderClient, BoletoRepository boletoRepository, BoletoMapper boletoMapper) {
        this.santanderClient = santanderClient;
        this.boletoRepository = boletoRepository;
        this.boletoMapper = boletoMapper;
    }

    public BoletoResponse createBoleto(BoletoRequest request) {
        BoletoResponse response = santanderClient.createBoleto(request);
        Boleto boleto = boletoMapper.toEntity(response);
        boletoRepository.save(boleto);
        return response;
    }

    public Optional<Boleto> findBoletoByIdentifiers(String environment, String nsuCode, String nsuDate,
                                                     String covenantCode, String bankNumber) {
        return boletoRepository.findByEnvironmentAndNsuCodeAndNsuDateAndCovenantCodeAndBankNumber(
            environment, nsuCode, nsuDate, covenantCode, bankNumber);
    }

    public BoletoResponse updateBoleto(BoletoRequest request) {
        BoletoResponse response = santanderClient.updateBoleto(null, request);
        Boleto boleto = boletoMapper.toEntity(response);
        boletoRepository.save(boleto);
        return response;
    }

    public BankSlipListResponse listBoletos(BankSlipListQuery query) {
        return santanderClient.listBoletos(query);
    }

    public BoletoResponse getBoletoById(String boletoId) {
        return santanderClient.getBoletoById(boletoId);
    }

    public Page<BoletoResponse> listBills(int page, int size) {
        return santanderClient.listBills(page, size);
    }

    public BoletoResponse getBillById(String billId) {
        return santanderClient.getBillById(billId);
    }

    public BoletoResponse getBillByNumber(String bankNumber, String beneficiaryCode) {
        return santanderClient.getBillByNumber(bankNumber, beneficiaryCode);
    }

    public String generatePdf(String billId) {
        return generatePdf(billId, null);
    }

    public String generatePdf(String billId, Long payerDocumentNumber) {
        if (santanderClient instanceof com.projeto.boleto.client.RestSantanderClient) {
            return ((com.projeto.boleto.client.RestSantanderClient) santanderClient)
                .generatePdf(billId, payerDocumentNumber);
        }
        return santanderClient.generatePdf(billId);
    }
}
