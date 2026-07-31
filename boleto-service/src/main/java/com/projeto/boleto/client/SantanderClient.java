package com.projeto.boleto.client;

import com.projeto.boleto.model.BankSlipListQuery;
import com.projeto.boleto.model.BankSlipListResponse;
import com.projeto.boleto.model.BoletoRequest;
import com.projeto.boleto.model.BoletoResponse;
import org.springframework.data.domain.Page;

/**
 * Interface for Santander Boleto API client operations.
 *
 * This interface defines the contract for interacting with the Santander Boleto API.
 * Implementations can target either mock or real Santander endpoints, with only URL and
 * credentials changing between environments.
 *
 * @author Boleto Service Team
 * @version 1.0
 */
public interface SantanderClient {

    /**
     * Creates a new boleto in the Santander system.
     *
     * @param request the {@link BoletoRequest} containing boleto creation details
     * @return the {@link BoletoResponse} containing the created boleto information
     * @throws RuntimeException if the creation fails
     */
    BoletoResponse createBoleto(BoletoRequest request);

    /**
     * Updates an existing boleto in the Santander system.
     *
     * @param boletoId the unique identifier of the boleto to update
     * @param request the {@link BoletoRequest} containing updated boleto details
     * @return the {@link BoletoResponse} containing the updated boleto information
     * @throws RuntimeException if the update fails or boleto is not found
     */
    BoletoResponse updateBoleto(String boletoId, BoletoRequest request);

    /**
     * Retrieves a filtered and paginated list of boletos from the Santander system.
     *
     * @param query the {@link BankSlipListQuery} containing filter and pagination parameters
     * @return a {@link BankSlipListResponse} containing filtered boletos
     * @throws RuntimeException if the retrieval fails
     */
    BankSlipListResponse<BoletoResponse> listBoletos(BankSlipListQuery query);

    /**
     * Retrieves a specific boleto by its unique identifier.
     *
     * @param boletoId the unique identifier of the boleto to retrieve
     * @return the {@link BoletoResponse} containing boleto information
     * @throws RuntimeException if the boleto is not found or retrieval fails
     */
    BoletoResponse getBoletoById(String boletoId);

    /**
     * Retrieves a paginated list of bills from the Santander system.
     *
     * @param page the zero-indexed page number
     * @param size the number of items per page
     * @return a {@link Page} containing {@link BoletoResponse} objects representing bills
     * @throws RuntimeException if the retrieval fails
     */
    Page<BoletoResponse> listBills(int page, int size);

    /**
     * Retrieves a specific bill by its unique identifier.
     *
     * @param billId the unique identifier of the bill to retrieve
     * @return the {@link BoletoResponse} containing bill information
     * @throws RuntimeException if the bill is not found or retrieval fails
     */
    BoletoResponse getBillById(String billId);

    /**
     * Retrieves a bill by bankNumber and beneficiaryCode.
     *
     * @param bankNumber the bank slip number
     * @param beneficiaryCode the beneficiary code (covenant code)
     * @return the {@link BoletoResponse} containing bill information
     * @throws RuntimeException if the bill is not found or retrieval fails
     */
    BoletoResponse getBillByNumber(String bankNumber, String beneficiaryCode);

    /**
     * Generates a PDF for a specific bill and returns its download URL.
     *
     * @param billId the unique identifier of the bill for which to generate the PDF
     * @return the URL string pointing to the generated PDF file
     * @throws RuntimeException if PDF generation fails
     */
    String generatePdf(String billId);
}
