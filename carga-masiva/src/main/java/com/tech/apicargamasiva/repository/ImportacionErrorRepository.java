package com.tech.apicargamasiva.repository;

import com.tech.apicargamasiva.model.ImportacionError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportacionErrorRepository extends JpaRepository<ImportacionError, Long> {

    /**
     * Busca todos los errores asociados a un job específico.
     */
    List<ImportacionError> findByJobId(String jobId);

    /**
     * Cuenta la cantidad de errores de un job.
     */
    long countByJobId(String jobId);

    /**
     * Borra todos los errores asociados a un job específico.
     */
    void deleteByJobId(String jobId);
}