package com.projeto.boleto.client;

import com.projeto.boleto.model.BankSlipListQuery;
import com.projeto.boleto.model.BankSlipListResponse;
import com.projeto.boleto.model.BoletoRequest;
import com.projeto.boleto.model.BoletoResponse;
import com.projeto.boleto.model.GeneratePdfRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST implementation of the Santander Client for Boleto API operations.
 * Uses Spring's RestTemplate to make HTTP calls to Santander endpoints.
 * All requests are authenticated via the OAuthClientHttpInterceptor registered with RestTemplate.
 *
 * @author Boleto Service Team
 * @version 1.0
 */
@Component
public class RestSantanderClient implements SantanderClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String workspaceId;

    /**
     * Constructs a new RestSantanderClient with injected dependencies.
     *
     * @param restTemplate the REST template for HTTP operations
     * @param baseUrl the base URL for Santander API
     * @param workspaceId the workspace ID for API operations
     */
    public RestSantanderClient(
            RestTemplate restTemplate,
            @Value("${santander.api.base-url}") String baseUrl,
            @Value("${santander.api.workspace-id}") String workspaceId) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.workspaceId = workspaceId;
    }

    /**
     * Creates a new boleto in the Santander system.
     *
     * @param request the BoletoRequest containing boleto details
     * @return the created BoletoResponse
     */
    @Override
    public BoletoResponse createBoleto(BoletoRequest request) {
        String url = baseUrl + "/collection_bill_management/v2/workspaces/" + workspaceId + "/bank_slips";
        ResponseEntity<BoletoResponse> response = restTemplate.postForEntity(url, request, BoletoResponse.class);
        return response.getBody();
    }

    /**
     * Updates an existing boleto in the Santander system.
     * Uses PUT with the composite boletoId (environment.nsuCode.nsuDate.covenantCode.bankNumber)
     *
     * @param boletoId the composite identifier (environment.nsuCode.nsuDate.covenantCode.bankNumber)
     * @param request the BoletoRequest with updated details
     * @return the updated BoletoResponse
     */
    @Override
    public BoletoResponse updateBoleto(String boletoId, BoletoRequest request) {
        String url = baseUrl + "/collection_bill_management/v2/workspaces/" + workspaceId + "/bank_slips";
        HttpEntity<BoletoRequest> entity = new HttpEntity<>(request);
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.PATCH, entity, String.class);

        if (response.getStatusCode().is5xxServerError()) {
            throw new RuntimeException("Failed to update boleto in Santander API");
        }

        BoletoResponse boletoResponse = new BoletoResponse(
            boletoId,
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
            null, null, null, null, null, null, null, null
        );
        return boletoResponse;
    }

    /**
     * Retrieves a filtered and paginated list of boletos from the Santander system.
     *
     * @param query the BankSlipListQuery containing filter and pagination parameters
     * @return a BankSlipListResponse containing filtered boletos
     */
    public BankSlipListResponse<BoletoResponse> listBoletos(BankSlipListQuery query) {
        StringBuilder url = new StringBuilder(baseUrl + "/collection_bill_management/v2/workspaces/" + workspaceId + "/bank_slips");
        url.append("?_limit=").append(query.limit());
        url.append("&_offset=").append(query.offset());
        url.append("&status=").append(query.status());

        if (query.bankNumber() != null) {
            url.append("&bankNumber=").append(query.bankNumber());
        }
        if (query.clientNumber() != null) {
            url.append("&clientNumber=").append(query.clientNumber());
        }
        if (query.dueDateInitial() != null) {
            url.append("&dueDateInitial=").append(query.dueDateInitial());
        }
        if (query.dueDateFinal() != null) {
            url.append("&dueDateFinal=").append(query.dueDateFinal());
        }
        if (query.paymentDateInitial() != null) {
            url.append("&paymentDateInitial=").append(query.paymentDateInitial());
        }
        if (query.paymentDateFinal() != null) {
            url.append("&paymentDateFinal=").append(query.paymentDateFinal());
        }

        ResponseEntity<BankSlipListResponse> response = restTemplate.exchange(
                url.toString(), HttpMethod.GET, null, new ParameterizedTypeReference<BankSlipListResponse>() {});
        return response.getBody();
    }

    /**
     * Retrieves a specific boleto by its unique identifier.
     *
     * @param boletoId the unique identifier of the boleto to retrieve
     * @return the BoletoResponse containing boleto information
     */
    @Override
    public BoletoResponse getBoletoById(String boletoId) {
        String url = baseUrl + "/collection_bill_management/v2/workspaces/" + workspaceId + "/bank_slips/" + boletoId;
        HttpEntity<?> entity = new HttpEntity<>((Object) null);
        ResponseEntity<BoletoResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, BoletoResponse.class);
        return response.getBody();
    }

    /**
     * Retrieves a paginated list of bills from the Santander system.
     *
     * @param page the zero-indexed page number
     * @param size the number of items per page
     * @return a Page containing BoletoResponse objects representing bills
     */
    @Override
    public Page<BoletoResponse> listBills(int page, int size) {
        String url = baseUrl + "/collection_bill_management/v2/bills?page=" + page + "&size=" + size;
        ResponseEntity<Page<BoletoResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<Page<BoletoResponse>>() {});
        return response.getBody() != null ? response.getBody() : Page.empty();
    }

    /**
     * Retrieves a specific bill by its unique identifier.
     *
     * @param billId the unique identifier of the bill to retrieve
     * @return the BoletoResponse containing bill information
     */
    @Override
    public BoletoResponse getBillById(String billId) {
        String url = baseUrl + "/collection_bill_management/v2/bills/" + billId;
        HttpEntity<?> entity = new HttpEntity<>((Object) null);
        ResponseEntity<BoletoResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, BoletoResponse.class);
        return response.getBody();
    }

    /**
     * Retrieves a bill by bankNumber and beneficiaryCode.
     *
     * @param bankNumber the bank slip number
     * @param beneficiaryCode the beneficiary code (covenant code)
     * @return the BoletoResponse containing bill information
     */
    @Override
    public BoletoResponse getBillByNumber(String bankNumber, String beneficiaryCode) {
        String url = baseUrl + "/collection_bill_management/v2/bills?bankNumber=" + bankNumber + "&beneficiaryCode=" + beneficiaryCode;
        ResponseEntity<BankSlipListResponse<BoletoResponse>> response = restTemplate.exchange(
            url, HttpMethod.GET, null,
            new ParameterizedTypeReference<BankSlipListResponse<BoletoResponse>>() {});
        BankSlipListResponse<BoletoResponse> result = response.getBody();
        if (result != null && result.data() != null && !result.data().isEmpty()) {
            return result.data().get(0);
        }
        throw new RuntimeException("Bill not found with bankNumber: " + bankNumber + " and beneficiaryCode: " + beneficiaryCode);
    }

    /**
     * Generates a PDF for a specific bill and returns its download URL.
     *
     * @param billId the unique identifier of the bill for PDF generation
     * @return the URL string pointing to the generated PDF file
     */
    @Override
    public String generatePdf(String billId) {
        return generatePdf(billId, null);
    }

    public String generatePdf(String billId, Long payerDocumentNumber) {
        String url = baseUrl + "/collection_bill_management/v2/bills/" + billId + "/bank_slips";
        GeneratePdfRequest request = new GeneratePdfRequest(payerDocumentNumber);
        HttpEntity<GeneratePdfRequest> entity = new HttpEntity<>(request);

        try {
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, java.util.Map.class);

            if (response.getBody() != null && response.getBody().containsKey("link")) {
                return (String) response.getBody().get("link");
            }
            throw new RuntimeException("Failed to generate PDF for bill: " + billId);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw extractSantanderError(ex);
        }
    }

    private SantanderApiException extractSantanderError(Exception ex) {
        String errorBody = null;
        int status = 500;
        String message = "Internal server error";
        String error = ex.getMessage();

        if (ex instanceof HttpClientErrorException) {
            HttpClientErrorException httpEx = (HttpClientErrorException) ex;
            errorBody = httpEx.getResponseBodyAsString();
            status = httpEx.getStatusCode().value();
        } else if (ex instanceof HttpServerErrorException) {
            HttpServerErrorException httpEx = (HttpServerErrorException) ex;
            errorBody = httpEx.getResponseBodyAsString();
            status = httpEx.getStatusCode().value();
        }

        if (errorBody != null && !errorBody.isEmpty()) {
            message = extractField(errorBody, "message", message);
            error = extractField(errorBody, "error", error);
        }

        return new SantanderApiException(status, message, error);
    }

    private String extractField(String json, String fieldName, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return defaultValue;
    }

}
