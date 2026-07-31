package com.santander.mock.repository;

import com.santander.mock.model.Boleto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletoMockRepository extends MongoRepository<Boleto, String> {
    Page<Boleto> findAll(Pageable pageable);
}
