package com.tech.apicargamasiva.repository;

import com.tech.apicargamasiva.model.Sueldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SueldoRepository extends JpaRepository<Sueldo, Long> {
    List<Sueldo> findByNumeroEmpleado(String numeroEmpleado);

    @Query("SELECT s FROM Sueldo s WHERE s.periodoPago = ?1")
    List<Sueldo> findByPeriodo(String periodo);
}