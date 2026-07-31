package com.santander.mock.service;

import com.santander.mock.model.*;
import com.santander.mock.repository.BoletoMockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class BoletoMockService {

    private final BoletoMockRepository repository;
    private final MongoTemplate mongoTemplate;

    public BoletoMockService(BoletoMockRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public BoletoResponse createBoleto(BoletoRequest request) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String entryDate = now.format(formatter);

        String barcode = generateBarcode(request.bankNumber(), request.nsuCode());
        String digitableLine = generateDigitableLine(barcode);
        String qrCodePix = generateQrCodePix();
        String qrCodeUrl = "pix.santander.com.br/qr/v2/cobv/" + UUID.randomUUID().toString().substring(0, 20);

        Boleto boleto = new Boleto(
            id,
            request.environment(),
            request.nsuCode(),
            request.nsuDate(),
            request.covenantCode(),
            request.bankNumber(),
            request.clientNumber(),
            request.dueDate(),
            request.issueDate(),
            request.participantCode(),
            request.nominalValue(),
            request.payer(),
            request.beneficiary(),
            request.documentKind(),
            request.discount(),
            request.finePercentage(),
            request.fineQuantityDays(),
            request.interestPercentage(),
            request.deductionValue(),
            request.protestType(),
            request.protestQuantityDays(),
            request.writeOffQuantityDays(),
            request.paymentType(),
            request.parcelsQuantity(),
            request.valueType(),
            request.minValueOrPercentage(),
            request.maxValueOrPercentage(),
            request.iofPercentage(),
            request.sharing(),
            request.key(),
            request.txId(),
            request.messages(),
            barcode,
            digitableLine,
            entryDate,
            qrCodePix,
            qrCodeUrl,
            "ATIVO",
            null,
            null,
            now.toString(),
            now.toString()
        );

        Boleto savedBoleto = repository.save(boleto);
        return toBoletoResponse(savedBoleto);
    }

    public BankSlipListResponse<BoletoResponse> listBoletos(BankSlipListQuery filter) {
        Criteria criteria = Criteria.where("status").is(filter.status());

        if (filter.bankNumber() != null) {
            criteria = criteria.and("bankNumber").is(filter.bankNumber());
        }
        if (filter.clientNumber() != null) {
            criteria = criteria.and("clientNumber").is(filter.clientNumber());
        }
        if (filter.dueDateInitial() != null || filter.dueDateFinal() != null) {
            Criteria dueDateCriteria = Criteria.where("dueDate");
            if (filter.dueDateInitial() != null) {
                dueDateCriteria = dueDateCriteria.gte(filter.dueDateInitial());
            }
            if (filter.dueDateFinal() != null) {
                dueDateCriteria = dueDateCriteria.lte(filter.dueDateFinal());
            }
            criteria = criteria.andOperator(dueDateCriteria);
        }
        if (filter.paymentDateInitial() != null || filter.paymentDateFinal() != null) {
            Criteria paymentDateCriteria = Criteria.where("payment.date");
            if (filter.paymentDateInitial() != null) {
                paymentDateCriteria = paymentDateCriteria.gte(filter.paymentDateInitial());
            }
            if (filter.paymentDateFinal() != null) {
                paymentDateCriteria = paymentDateCriteria.lte(filter.paymentDateFinal());
            }
            criteria = criteria.andOperator(paymentDateCriteria);
        }

        Query countQuery = Query.query(criteria);
        long totalElements = mongoTemplate.count(countQuery, Boleto.class);

        Query pageQuery = Query.query(criteria).skip((long) filter.offset()).limit(filter.limit());
        var content = mongoTemplate.find(pageQuery, Boleto.class).stream()
            .map(this::toBoletoResponse)
            .toList();

        int totalPages = filter.limit() == 0 ? 0
            : (int) Math.ceil((double) totalElements / filter.limit());
        int pageNumber = filter.limit() == 0 ? 0 : filter.offset() / filter.limit();

        PageableInfo pageableInfo = new PageableInfo(
            filter.limit(), filter.offset(), pageNumber, content.size(), totalPages, totalElements);

        return new BankSlipListResponse<>(pageableInfo, content);
    }

    public Page<BoletoResponse> listAllUnfiltered(Pageable pageable) {
        return repository.findAll(pageable)
            .map(this::toBoletoResponse);
    }

    public BoletoResponse getBoletoById(String id) {
        Boleto boleto = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Boleto not found: " + id));
        return toBoletoResponse(boleto);
    }

    public BoletoResponse getBoletoByBankNumberAndCovenantCode(String bankNumber, String covenantCode) {
        Query query = Query.query(
            Criteria.where("bankNumber").is(bankNumber)
                .and("covenantCode").is(covenantCode)
        );
        Boleto boleto = mongoTemplate.findOne(query, Boleto.class);
        if (boleto == null) {
            throw new RuntimeException("Boleto not found with bankNumber: " + bankNumber + " and covenantCode: " + covenantCode);
        }
        return toBoletoResponse(boleto);
    }

    public BoletoResponse getBoletoByIdentifiers(String environment, String nsuCode, String nsuDate,
                                                 String covenantCode, String bankNumber) {
        Query query = Query.query(
            Criteria.where("environment").is(environment)
                .and("nsuCode").is(nsuCode)
                .and("nsuDate").is(nsuDate)
                .and("covenantCode").is(covenantCode)
                .and("bankNumber").is(bankNumber)
        );
        Boleto boleto = mongoTemplate.findOne(query, Boleto.class);
        if (boleto == null) {
            throw new RuntimeException("Boleto not found with identifiers: " + environment + "." + nsuCode + "." + nsuDate + "." + covenantCode + "." + bankNumber);
        }
        return toBoletoResponse(boleto);
    }

    public BoletoResponse updateBoleto(String id, BoletoRequest request) {
        Boleto boleto = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Boleto not found: " + id));

        LocalDateTime now = LocalDateTime.now();
        String barcode = generateBarcode(request.bankNumber(), request.nsuCode());
        String digitableLine = generateDigitableLine(barcode);
        String qrCodePix = generateQrCodePix();
        String qrCodeUrl = "pix.santander.com.br/qr/v2/cobv/" + UUID.randomUUID().toString().substring(0, 20);

        String newStatus = request.status() != null ? request.status() : boleto.status();
        String newStatusComplement = request.statusComplement() != null ? request.statusComplement() : boleto.statusComplement();
        Payment newPayment = request.payment() != null ? request.payment() : boleto.payment();

        Boleto updatedBoleto = new Boleto(
            boleto.id(),
            request.environment(),
            request.nsuCode(),
            request.nsuDate(),
            request.covenantCode(),
            request.bankNumber(),
            request.clientNumber(),
            request.dueDate(),
            request.issueDate(),
            request.participantCode(),
            request.nominalValue(),
            request.payer(),
            request.beneficiary(),
            request.documentKind(),
            request.discount(),
            request.finePercentage(),
            request.fineQuantityDays(),
            request.interestPercentage(),
            request.deductionValue(),
            request.protestType(),
            request.protestQuantityDays(),
            request.writeOffQuantityDays(),
            request.paymentType(),
            request.parcelsQuantity(),
            request.valueType(),
            request.minValueOrPercentage(),
            request.maxValueOrPercentage(),
            request.iofPercentage(),
            request.sharing(),
            request.key(),
            request.txId(),
            request.messages(),
            barcode,
            digitableLine,
            boleto.entryDate(),
            qrCodePix,
            qrCodeUrl,
            newStatus,
            newStatusComplement,
            newPayment,
            boleto.createdAt(),
            now.toString()
        );

        Boleto savedBoleto = repository.save(updatedBoleto);
        return toBoletoResponse(savedBoleto);
    }

    public void deleteBoleto(String id) {
        repository.deleteById(id);
    }

    private BoletoResponse toBoletoResponse(Boleto boleto) {
        return new BoletoResponse(
            boleto.id(),
            boleto.environment(),
            boleto.nsuCode(),
            boleto.nsuDate(),
            boleto.covenantCode(),
            boleto.bankNumber(),
            boleto.clientNumber(),
            boleto.dueDate(),
            boleto.issueDate(),
            boleto.participantCode(),
            boleto.nominalValue(),
            boleto.payer(),
            boleto.beneficiary(),
            boleto.documentKind(),
            boleto.discount(),
            boleto.finePercentage(),
            boleto.fineQuantityDays(),
            boleto.interestPercentage(),
            boleto.deductionValue(),
            boleto.protestType(),
            boleto.protestQuantityDays(),
            boleto.writeOffQuantityDays(),
            boleto.paymentType(),
            boleto.parcelsQuantity(),
            boleto.valueType(),
            boleto.minValueOrPercentage(),
            boleto.maxValueOrPercentage(),
            boleto.iofPercentage(),
            boleto.sharing(),
            boleto.key(),
            boleto.txId(),
            boleto.messages(),
            boleto.barcode(),
            boleto.digitableLine(),
            boleto.entryDate(),
            boleto.qrCodePix(),
            boleto.qrCodeUrl(),
            boleto.status(),
            boleto.statusComplement(),
            boleto.payment()
        );
    }

    private String generateBarcode(String bankNumber, String nsuCode) {
        return String.format("03396939%s%s000000000%s", bankNumber, nsuCode, UUID.randomUUID().toString().substring(0, 20));
    }

    private String generateDigitableLine(String barcode) {
        if (barcode.length() < 47) return barcode;
        return barcode.substring(0, 4) + barcode.substring(32, 47) + "0" + barcode.substring(4, 32) + "0" + barcode.substring(0, 4);
    }

    private String generateQrCodePix() {
        return "00020101021226920014br.gov.bcb.pix2570pix.santander.com.br/qr/v2/cobv/" +
               UUID.randomUUID().toString().replace("-", "").substring(0, 32) +
               "52040000530398654041.005802BR5925TESTE SANTANDER API 16009SAO PAULO62070503***63041110";
    }
}
