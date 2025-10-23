package com.tech.apicargamasiva.service;

import com.tech.apicargamasiva.dto.ImportacionErrorDTO;
import com.tech.apicargamasiva.model.ImportacionError;
import com.tech.apicargamasiva.repository.ImportacionErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacionErrorService {

    @Autowired
    private ImportacionErrorRepository repository;

    public void guardarErrores(List<ImportacionErrorDTO> errores) {
        if (errores == null || errores.isEmpty()) {
            log.debug("No hay errores que guardar.");
            return;
        }

        try {
            // Convertimos los DTOs a entidades
            List<ImportacionError> entidades = errores.stream()
                    .map(this::toEntity)
                    .toList();

            // Guardamos en batch
            repository.saveAll(entidades);

            log.info("💾 Se guardaron {} errores de importación.", entidades.size());
        } catch (Exception e) {
            log.error("❌ Error al guardar errores de importación: {}", e.getMessage(), e);
        }
    }

    public void guardarError(ImportacionError error) {
        repository.save(error);
    }

    public List<ImportacionError> obtenerErroresPorJob(String jobId) {
        return repository.findByJobId(jobId);
    }

    public long contarErroresPorJob(String jobId) {
        return repository.countByJobId(jobId);
    }

    public void eliminarErroresPorJob(String jobId) {
        repository.deleteByJobId(jobId);
    }

    private ImportacionError toEntity(ImportacionErrorDTO dto) {
        return ImportacionError.builder()
                .jobId(dto.getJobId())
                .rowNumber(dto.getRowNumber())
                .numeroEmpleado(dto.getNumeroEmpleado())
                .errorMessage(dto.getErrorMessage())
                .rawData(dto.getRawData())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
