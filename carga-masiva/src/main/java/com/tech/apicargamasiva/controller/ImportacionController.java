package com.tech.apicargamasiva.controller;
import com.tech.apicargamasiva.dto.ApiResponse;
import com.tech.apicargamasiva.dto.EstadisticasDTO;
import com.tech.apicargamasiva.dto.ImportacionResponse;
import com.tech.apicargamasiva.dto.JobStatusDTO;
import com.tech.apicargamasiva.model.ImportacionError;
import com.tech.apicargamasiva.model.ImportacionJob;
import com.tech.apicargamasiva.repository.ImportacionErrorRepository;
import com.tech.apicargamasiva.repository.ImportacionJobRepository;
import com.tech.apicargamasiva.service.ImportacionService;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/importacion")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
@Validated
public class ImportacionController {

    @Autowired
    private ImportacionService importacionService;

    @Autowired
    private ImportacionJobRepository jobRepository;

    @Autowired
    private ImportacionErrorRepository errorRepository;

    /**
     * Endpoint para subir y procesar archivo Excel
     *
     * @param file Archivo Excel (.xlsx)
     * @param userEmail Email del usuario para notificaciones
     * @return Respuesta con el Job ID creado
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportacionResponse>> subirExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userEmail") @Email(message = "Email inválido") String userEmail) {

        try {
            log.info("Recibiendo archivo: {} ({} bytes) de usuario: {}",
                    file.getOriginalFilename(),
                    file.getSize(),
                    userEmail);

            // Validaciones básicas
            if (file.isEmpty()) {
                log.warn("Intento de subir archivo vacío");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "El archivo está vacío"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                log.warn("Formato de archivo inválido: {}", filename);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST,
                                "Solo se aceptan archivos .xlsx"));
            }

            // Validar tamaño (100MB)
            long maxSize = 100L * 1024 * 1024; // 100MB en bytes
            if (file.getSize() > maxSize) {
                log.warn("Archivo excede tamaño máximo: {} bytes", file.getSize());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST,
                                "El archivo excede el tamaño máximo de 100MB"));
            }

            // Iniciar importación asíncrona
            ImportacionResponse response = importacionService.iniciarImportacion(file, userEmail);

            log.info("Importación iniciada exitosamente. Job ID: {}", response.getJobId());

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(response,
                            "Importación iniciada exitosamente. Recibirás un email al finalizar."));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage()));

        } catch (Exception e) {
            log.error("Error inesperado procesando archivo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error procesando el archivo. Por favor, intente nuevamente."));
        }
    }

    /**
     * Obtener el estado de un job específico
     *
     * @param jobId ID del job
     * @return Estado actual del job
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ApiResponse<JobStatusDTO>> obtenerEstatus(
            @PathVariable String jobId) {

        try {
            log.debug("Consultando estatus del job: {}", jobId);

            JobStatusDTO status = importacionService.obtenerEstatus(jobId);

            return ResponseEntity.ok(
                    ApiResponse.success(status, "Estatus obtenido exitosamente")
            );

        } catch (RuntimeException e) {
            log.error("Error obteniendo estatus del job {}: {}", jobId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND,
                            "Job no encontrado: " + jobId));

        } catch (Exception e) {
            log.error("Error inesperado obteniendo estatus: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error obteniendo el estatus del job"));
        }
    }

    /**
     * Listar todos los jobs, opcionalmente filtrados por email
     *
     * @param userEmail Email para filtrar (opcional)
     * @return Lista de jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobStatusDTO>>> listarJobs(
            @RequestParam(required = false) String userEmail) {

        try {
            log.debug("Listando jobs. Email filter: {}", userEmail);

            List<ImportacionJob> jobs;

            if (userEmail != null && !userEmail.trim().isEmpty()) {
                jobs = jobRepository.findByUserEmailOrderByCreatedAtDesc(userEmail.trim());
            } else {
                jobs = jobRepository.findAll();
            }

            List<JobStatusDTO> jobsDTO = jobs.stream()
                    .map(this::convertirAJobStatusDTO)
                    .collect(Collectors.toList());

            log.info("Se encontraron {} jobs", jobsDTO.size());

            return ResponseEntity.ok(
                    ApiResponse.success(jobsDTO,
                            String.format("Se encontraron %d importaciones", jobsDTO.size()))
            );

        } catch (Exception e) {
            log.error("Error listando jobs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error obteniendo lista de jobs"));
        }
    }

    /**
     * Obtener lista de errores de un job específico
     *
     * @param jobId ID del job
     * @return Lista de errores encontrados durante la importación
     */
    @GetMapping("/errors/{jobId}")
    public ResponseEntity<ApiResponse<List<ImportacionError>>> obtenerErrores(
            @PathVariable String jobId) {

        try {
            log.debug("Obteniendo errores del job: {}", jobId);

            // Verificar que el job exista
            if (!jobRepository.existsById(jobId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND,
                                "Job no encontrado: " + jobId));
            }

            List<ImportacionError> errores = errorRepository.findByJobId(jobId);

            log.info("Se encontraron {} errores para el job {}", errores.size(), jobId);

            return ResponseEntity.ok(
                    ApiResponse.success(errores,
                            String.format("Se encontraron %d errores", errores.size()))
            );

        } catch (Exception e) {
            log.error("Error obteniendo errores del job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error obteniendo los errores del job"));
        }
    }

    /**
     * Obtener conteo de errores de un job
     *
     * @param jobId ID del job
     * @return Cantidad de errores
     */
    @GetMapping("/errors/{jobId}/count")
    public ResponseEntity<ApiResponse<Long>> contarErrores(
            @PathVariable String jobId) {

        try {
            Long count = errorRepository.countByJobId(jobId);

            return ResponseEntity.ok(
                    ApiResponse.success(count, "Conteo de errores obtenido")
            );

        } catch (Exception e) {
            log.error("Error contando errores del job {}: {}", jobId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error contando errores"));
        }
    }

    /**
     * Eliminar un job y sus errores asociados
     *
     * @param jobId ID del job a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<Void>> eliminarJob(
            @PathVariable String jobId) {

        try {
            log.info("Eliminando job: {}", jobId);

            if (!jobRepository.existsById(jobId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND,
                                "Job no encontrado: " + jobId));
            }

            // Spring eliminará automáticamente los errores por la FK con ON DELETE CASCADE
            jobRepository.deleteById(jobId);

            log.info("Job {} eliminado exitosamente", jobId);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Job eliminado exitosamente")
            );

        } catch (Exception e) {
            log.error("Error eliminando job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error eliminando el job"));
        }
    }

    /**
     * Cancelar un job en proceso
     *
     * @param jobId ID del job a cancelar
     * @return Confirmación de cancelación
     */
    @PutMapping("/job/{jobId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelarJob(
            @PathVariable String jobId) {

        try {
            log.info("Cancelando job: {}", jobId);

            ImportacionJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job no encontrado"));

            if (job.getStatus() == ImportacionJob.JobStatus.COMPLETADO ||
                    job.getStatus() == ImportacionJob.JobStatus.ERROR) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST,
                                "No se puede cancelar un job que ya finalizó"));
            }

            job.setStatus(ImportacionJob.JobStatus.CANCELADO);
            jobRepository.save(job);

            log.info("Job {} cancelado exitosamente", jobId);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Job cancelado exitosamente")
            );

        } catch (RuntimeException e) {
            log.error("Job no encontrado: {}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND, e.getMessage()));

        } catch (Exception e) {
            log.error("Error cancelando job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error cancelando el job"));
        }
    }

    /**
     * Obtener estadísticas generales de importaciones
     *
     * @return Estadísticas agregadas
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EstadisticasDTO>> obtenerEstadisticas() {
        try {
            long totalJobs = jobRepository.count();
            long completados = jobRepository.findByStatus(ImportacionJob.JobStatus.COMPLETADO).size();
            long enProceso = jobRepository.findByStatus(ImportacionJob.JobStatus.EN_PROCESO).size();
            long conErrores = jobRepository.findByStatus(ImportacionJob.JobStatus.ERROR).size();

            EstadisticasDTO stats = EstadisticasDTO.builder()
                    .total(totalJobs)
                    .completados(completados)
                    .enProceso(enProceso)
                    .errores(conErrores)
                    .build();

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "Estadísticas obtenidas")
            );

        } catch (Exception e) {
            log.error("Error obteniendo estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error obteniendo estadísticas"));
        }
    }

    /**
     * Convierte un ImportacionJob a DTO
     */
    private JobStatusDTO convertirAJobStatusDTO(ImportacionJob job) {
        double progreso = job.getTotalRegistros() > 0
                ? (job.getProcesados() * 100.0) / job.getTotalRegistros()
                : 0;

        return JobStatusDTO.builder()
                .jobId(job.getId())
                .filename(job.getFilename())
                .status(job.getStatus())
                .totalRegistros(job.getTotalRegistros())
                .procesados(job.getProcesados())
                .exitosos(job.getExitosos())
                .errores(job.getErrores())
                .errorMessage(job.getErrorMessage())
                .progreso(progreso)
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}