package com.projeto.boleto.repository;

import com.projeto.boleto.model.Boleto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BoletoRepository extends MongoRepository<Boleto, String> {
    Page<Boleto> findAll(Pageable pageable);

    Optional<Boleto> findByEnvironmentAndNsuCodeAndNsuDateAndCovenantCodeAndBankNumber(
        String environment, String nsuCode, String nsuDate, String covenantCode, String bankNumber);
}
