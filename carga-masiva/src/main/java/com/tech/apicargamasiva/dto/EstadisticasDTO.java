package com.tech.apicargamasiva.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasDTO {
    private long total;
    private long completados;
    private long enProceso;
    private long errores;
}