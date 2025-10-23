package com.tech.apicargamasiva.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tech.apicargamasiva.config.RabbitMQConfig;
import com.tech.apicargamasiva.dto.ChunkMessage;
import com.tech.apicargamasiva.dto.ImportacionResponse;
import com.tech.apicargamasiva.dto.JobStatusDTO;
import com.tech.apicargamasiva.dto.ProgressUpdate;
import com.tech.apicargamasiva.model.ImportacionJob;
import com.tech.apicargamasiva.repository.ImportacionJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ImportacionService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private  RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // para LocalDateTime
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private ExcelService excelService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ImportacionJobRepository jobRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${importacion.chunk-size:1000}")
    private int chunkSize;

    @Value("${importacion.temp-directory:./temp-uploads}")
    private String tempDirectory;

    /**
     * Inicia la importación: guarda archivo, crea job y dispara procesamiento async
     */
    public ImportacionResponse iniciarImportacion(MultipartFile file, String userEmail) throws IOException {
        String jobId = UUID.randomUUID().toString();
        Path tempPath = guardarArchivo(file, jobId);

        if (!excelService.validarFormato(tempPath)) {
            throw new IllegalArgumentException("Formato de Excel inválido. Revise los headers.");
        }

        ImportacionJob job = ImportacionJob.builder()
                .id(jobId)
                .filename(file.getOriginalFilename())
                .userEmail(userEmail)
                .status(ImportacionJob.JobStatus.VALIDANDO)
                .build();

        jobRepository.save(job);
        redisTemplate.opsForValue().set("job:" + jobId, job, Duration.ofDays(7));

        procesarAsync(jobId, tempPath, userEmail);

        return ImportacionResponse.builder()
                .jobId(jobId)
                .message("Importación iniciada exitosamente")
                .statusUrl("/api/importacion/status/" + jobId)
                .build();
    }

    @Async("importacionExecutor")
    public void procesarAsync(String jobId, Path excelPath, String userEmail) {
        try {
            log.info("Iniciando procesamiento de job: {}", jobId);
            int totalRegistros = excelService.contarRegistros(excelPath);

            actualizarJob(jobId, ImportacionJob.JobStatus.EN_PROCESO, totalRegistros, 0, 0, 0);

            for (int i = 1; i <= totalRegistros; i += chunkSize) {
                ChunkMessage chunk = ChunkMessage.builder()
                        .jobId(jobId)
                        .filePath(excelPath.toString())
                        .startRow(i)
                        .endRow(Math.min(i + chunkSize - 1, totalRegistros))
                        .userEmail(userEmail)
                        .build();

                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, chunk);
                log.debug("Chunk enviado: {} - {}", chunk.getStartRow(), chunk.getEndRow());
            }

        } catch (Exception e) {
            log.error("Error procesando importación {}: {}", jobId, e.getMessage(), e);
            actualizarJob(jobId, ImportacionJob.JobStatus.ERROR, 0, 0, 0, 0);
            emailService.enviarEmailError(userEmail, jobId, e.getMessage());
        }
    }

    private Path guardarArchivo(MultipartFile file, String jobId) throws IOException {
        Path uploadDir = Paths.get(tempDirectory);
        if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(jobId + "_" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath;
    }

    /**
     * Actualiza un job en BD y Redis, y notifica progreso por WebSocket
     */
    public void actualizarJob(String jobId, ImportacionJob.JobStatus status, Integer total,
                              Integer procesados, Integer exitosos, Integer errores) {

        ImportacionJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job no encontrado"));

        job.setStatus(status);
        if (total != null) job.setTotalRegistros(total);
        if (procesados != null) job.setProcesados(procesados);
        if (exitosos != null) job.setExitosos(exitosos);
        if (errores != null) job.setErrores(errores);

        jobRepository.save(job);
        redisTemplate.opsForValue().set("job:" + jobId, job, Duration.ofDays(7));

        notificarProgreso(job);
    }

    private void notificarProgreso(ImportacionJob job) {
        double progreso = job.getTotalRegistros() > 0
                ? (job.getProcesados() * 100.0) / job.getTotalRegistros()
                : 0;

        ProgressUpdate update = ProgressUpdate.builder()
                .jobId(job.getId())
                .totalRegistros(job.getTotalRegistros())
                .procesados(job.getProcesados())
                .exitosos(job.getExitosos())
                .errores(job.getErrores())
                .progreso(progreso)
                .status(job.getStatus() != null ? job.getStatus().toString() : "DESCONOCIDO")
                .build();

        messagingTemplate.convertAndSend("/topic/importacion/" + job.getId(), update);
    }
    public JobStatusDTO obtenerEstatus(String jobId) {

        // 1. Obtener el objeto, el serializador ya intenta deserializarlo
        Object raw = redisTemplate.opsForValue().get(jobId);

        if (raw == null) {
            throw new RuntimeException("Job no encontrado: " + jobId);
        }

        // 2. Intentar el casteo directo al tipo esperado
        if (!(raw instanceof ImportacionJob)) {
            // Esto indica que el serializer no pudo deserializarlo al tipo correcto.
            // Podría ser un error de la clave __TypeId__ o datos corruptos.
            log.error("Tipo de objeto incorrecto en Redis. Esperado: ImportacionJob, Obtenido: {}",
                    raw.getClass().getName());
            throw new IllegalStateException("Datos de Job corruptos en Redis.");
        }

        ImportacionJob job = (ImportacionJob) raw;

        // 3. Devolver el DTO
        return new JobStatusDTO(job);
    }
}
