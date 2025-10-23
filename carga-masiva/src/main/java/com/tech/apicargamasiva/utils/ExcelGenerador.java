package com.tech.apicargamasiva.utils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.Date;
import java.util.Random;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExcelGenerador {

    public static void main(String[] args) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sueldos");

        // Listas más grandes para generar nombres aleatorios
        String[] nombresPila = {
                "Juan", "Ana", "Carlos", "Sofía", "Luis", "Elena", "Miguel", "Laura", "Javier", "Isabel",
                "Daniel", "María", "David", "Carmen", "Fernando", "Lucía", "Alejandro", "Teresa", "Sergio", "Paula"
        };
        String[] apellidos = {
                "Pérez", "López", "García", "Martínez", "Rodríguez", "Sánchez", "Fernández", "Gómez", "Díaz", "Vázquez",
                "Ruiz", "Hernández", "Moreno", "Jiménez", "Álvarez", "Gutiérrez", "Castillo", "Ortega", "Guerrero", "Reyes"
        };

        // Datos de ejemplo
        String[] puestos = {"Gerente", "Analista", "Desarrollador Senior", "Desarrollador Junior", "Diseñador"};
        String periodoPago = "Octubre 2025";
        String fechaPago = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Headers solicitados:
        String[] headers = {
                "numero_empleado", "nombre_completo", "puesto",
                "salario_base", "bonos", "deducciones",
                "salario_neto", "periodo_pago", "fecha_pago"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        Random random = new Random();

        // Generar 100 registros
        int limitItems = 10000;
        for (int i = 1; i <= limitItems; i++) {
            Row row = sheet.createRow(i);

            // Generar valores aleatorios para el cálculo
            int salarioBase = 8000 + random.nextInt(12000); // 8000-20000
            int bonos = random.nextInt(3000); // 0-3000
            int deducciones = random.nextInt(2500); // 0-2500

            // ⚠️ ASEGURANDO LA REGLA: salario_neto = salario_base + bonos - deducciones
            int salarioNeto = salarioBase + bonos - deducciones;

            // Generación de nombre aleatorio y combinación
            String nombrePila = nombresPila[random.nextInt(nombresPila.length)];
            String apellido1 = apellidos[random.nextInt(apellidos.length)];
            String apellido2 = apellidos[random.nextInt(apellidos.length)];
            String nombreCompleto = nombrePila + " " + apellido1 + " " + apellido2;

            String puesto = puestos[random.nextInt(puestos.length)];

            // Crear y llenar celdas
            row.createCell(0).setCellValue("EMP" + String.format("%03d", i)); // numero_empleado
            row.createCell(1).setCellValue(nombreCompleto); // nombre_completo (ahora más random)
            row.createCell(2).setCellValue(puesto); // puesto
            row.createCell(3).setCellValue(salarioBase); // salario_base
            row.createCell(4).setCellValue(bonos); // bonos
            row.createCell(5).setCellValue(deducciones); // deducciones
            row.createCell(6).setCellValue(salarioNeto); // salario_neto
            row.createCell(7).setCellValue(periodoPago); // periodo_pago
            row.createCell(8).setCellValue(fechaPago); // fecha_pago
        }

        // Ajustar ancho columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Guardar archivo
        String fileName = "sueldos_demo_" + new Date().getTime() + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            workbook.write(fos);
        }

        workbook.close();
        System.out.println("✅ Excel generado con " + limitItems + " registros (nombres más random): sueldos_final_random.xlsx");
    }
}