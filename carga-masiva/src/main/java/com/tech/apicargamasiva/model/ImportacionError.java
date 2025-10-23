package com.tech.apicargamasiva.model;


import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "importacion_errores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportacionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobId;
    private Integer rowNumber;
    private String numeroEmpleado;
    private String errorMessage;

    @Column(columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawData;
    private LocalDateTime createdAt;
}