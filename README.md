# 💼 Bulk Salary Import System

Sistema de importación masiva de sueldos desde archivos Excel con procesamiento asíncrono, notificaciones en tiempo real y manejo robusto de errores.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red.svg)](https://angular.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📋 Tabla de Contenidos

- [Descripción](#descripción)
- [Características](#características)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Uso](#uso)
- [API Endpoints](#api-endpoints)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Librerías y su Uso](#librerías-y-su-uso)

---

## 🎯 Descripción

**Bulk Salary Import System** es una aplicación empresarial diseñada para procesar importaciones masivas de información de sueldos desde archivos Excel (.xlsx), permitiendo cargar millones de registros sin bloquear la interfaz de usuario.

### ¿Qué hace este sistema?

1. **Carga archivos Excel** con información de sueldos de empleados
2. **Procesa de forma asíncrona** dividiendo el trabajo en chunks (fragmentos)
3. **Valida cada registro** antes de insertarlo en la base de datos
4. **Notifica en tiempo real** el progreso del procesamiento vía WebSocket
5. **Envía emails** cuando la importación finaliza (exitosa o con errores)
6. **Genera reportes** de errores para registros que fallaron la validación
7. **Proporciona resiliencia** con reintentos automáticos y circuit breakers

### Casos de Uso

- ✅ Migración masiva de nóminas de sistemas legacy
- ✅ Importación periódica de datos desde hojas de cálculo
- ✅ Procesamiento de nóminas de múltiples sucursales
- ✅ Auditoría y validación de información salarial

---

## ✨ Características

### Backend
- 🚀 **Procesamiento Asíncrono**: Los archivos se procesan en background sin bloquear al usuario
- 📊 **División en Chunks**: Archivos grandes se dividen en fragmentos de 1000 registros
- 🔄 **Procesamiento Paralelo**: Múltiples workers procesan chunks simultáneamente
- ✅ **Validación Robusta**: Valida tipos de datos, formatos y reglas de negocio
- 🔁 **Reintentos Automáticos**: Reintentos con backoff exponencial ante fallos temporales
- 🛡️ **Circuit Breaker**: Protección contra cascadas de fallos
- 📧 **Notificaciones Email**: Envío automático de resultados al finalizar
- 💾 **Bulk Inserts**: Inserciones masivas optimizadas con JDBC batch
- 🔍 **Tracking en Tiempo Real**: Seguimiento del progreso vía WebSocket
- 📝 **Logs Detallados**: Registro completo de todas las operaciones
- ⚡ **Caché con Redis**: Almacenamiento temporal del estado de los jobs

### Frontend
- 🎨 **Interfaz Intuitiva**: Drag & drop para subir archivos
- 📊 **Dashboard en Tiempo Real**: Visualización del progreso en vivo
- 📈 **Gráficas de Progreso**: Barras de progreso animadas
- 📋 **Historial de Importaciones**: Consulta de trabajos anteriores
- ⚠️ **Reporte de Errores**: Descarga de registros con problemas
- 🔔 **Notificaciones Push**: Alertas en tiempo real vía WebSocket

---

## 🏗️ Arquitectura

```
┌─────────────┐
│   Angular   │  Frontend (Puerto 4200)
│   Frontend  │  
└──────┬──────┘
       │ HTTP REST + WebSocket
       │
┌──────▼──────────────────────────────────────┐
│         Spring Boot Backend                 │
│              (Puerto 8080)                   │
│                                              │
│  ┌────────────┐  ┌──────────────┐          │
│  │ Controller │──│   Service    │          │
│  └────────────┘  └──────┬───────┘          │
│                         │                   │
│                  ┌──────▼───────┐           │
│                  │  RabbitMQ    │           │
│                  │  (Producer)  │           │
│                  └──────┬───────┘           │
└─────────────────────────┼───────────────────┘
                          │
                ┌─────────▼─────────┐
                │    RabbitMQ       │  Message Broker
                │   (Queue/DLQ)     │  (Puerto 5672)
                └─────────┬─────────┘
                          │
┌─────────────────────────┼───────────────────┐
│         Worker Consumers (Paralelo)         │
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │Consumer 1│  │Consumer 2│  │Consumer N│  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  │
│       │            │             │         │
│       └────────────┴─────────────┘         │
│                    │                        │
│         ┌──────────▼─────────┐              │
│         │  Bulk Insert (DB)  │              │
│         └────────────────────┘              │
└─────────────────────────────────────────────┘
           │              │
   ┌───────▼────┐  ┌──────▼──────┐
   │ PostgreSQL │  │    Redis    │
   │  (Puerto   │  │  (Puerto    │
   │   5432)    │  │   6379)     │
   └────────────┘  └─────────────┘
```

### Flujo de Procesamiento

1. **Usuario sube Excel** → Controller recibe el archivo
2. **Validación inicial** → Se verifica formato y headers
3. **Creación de Job** → Se genera un UUID único
4. **División en Chunks** → El archivo se divide en fragmentos
5. **Envío a RabbitMQ** → Cada chunk se envía como mensaje
6. **Workers consumen** → Múltiples consumers procesan en paralelo
7. **Validación y Insert** → Se valida y guarda cada registro
8. **Actualización de Progreso** → Redis + WebSocket notifican avance
9. **Finalización** → Email de confirmación con reporte

---

## 🛠️ Tecnologías

### Backend
| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| **Java** | 17 | Lenguaje de programación |
| **Spring Boot** | 3.2.0 | Framework principal |
| **Spring Data JPA** | 3.2.0 | ORM y gestión de BD |
| **PostgreSQL** | 15 | Base de datos principal |
| **Redis** | 7 | Cache y tracking de jobs |
| **RabbitMQ** | 3.12 | Message broker para procesamiento asíncrono |
| **Apache POI** | 5.2.5 | Lectura de archivos Excel |
| **Resilience4j** | 2.2.0 | Circuit breaker y reintentos |
| **Spring WebSocket** | 3.2.0 | Notificaciones en tiempo real |
| **Spring Mail** | 3.2.0 | Envío de emails |
| **Thymeleaf** | 3.2.0 | Templates HTML para emails |
| **Lombok** | 1.18.30 | Reducción de código boilerplate |
| **Jackson** | 2.15.0 | Serialización JSON |

### Frontend
| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| **Angular** | 17 | Framework SPA |
| **TypeScript** | 5.2 | Lenguaje tipado |
| **RxJS** | 7.8 | Programación reactiva |
| **STOMP.js** | 7.0 | Cliente WebSocket |
| **SockJS** | 1.6 | Fallback para WebSocket |
| **Bootstrap** | 5.3 | Framework CSS |
| **Font Awesome** | 6.0 | Iconos |

### DevOps
| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| **Docker** | 24+ | Containerización |
| **Docker Compose** | 3.8 | Orquestación local |
| **Maven** | 3.9+ | Gestión de dependencias |
| **MailHog** | latest | Testing de emails |

---

## 📦 Requisitos

### Desarrollo Local
- **Java JDK 17+**
- **Maven 3.9+**
- **Node.js 18+** y **npm 9+**
- **Docker** y **Docker Compose**
- **Git**

### Producción
- **Servidor con 4GB RAM** mínimo
- **PostgreSQL 15+**
- **Redis 7+**
- **RabbitMQ 3.12+**
- **Servidor SMTP** (para emails)

---

## 🚀 Instalación

### 1. Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/bulk-salary-import-system.git
cd bulk-salary-import-system
```

### 2. Levantar Infraestructura con Docker

```bash
# Iniciar PostgreSQL, Redis, RabbitMQ y MailHog
docker-compose up -d

# Verificar que todos los servicios estén corriendo
docker-compose ps
```

Servicios disponibles:
- **PostgreSQL**: `localhost:5432` (admin/admin123)
- **Redis**: `localhost:6379`
- **RabbitMQ Management**: `http://localhost:15672` (admin/admin123)
- **MailHog UI**: `http://localhost:8025`

### 3. Backend (Spring Boot)

```bash
# Compilar y ejecutar
cd sueldo-import
mvn clean install
mvn spring-boot:run

# La aplicación estará disponible en http://localhost:8080
```

### 4. Frontend (Angular)

```bash
# Instalar dependencias
cd sueldo-import-frontend
npm install

# Iniciar servidor de desarrollo
ng serve

# La aplicación estará disponible en http://localhost:4200
```

---

## ⚙️ Configuración

### Backend - application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sueldos_db
    username: admin
    password: admin123
    
  data:
    redis:
      host: localhost
      port: 6379
      
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123
    
  mail:
    host: localhost  # Cambiar en producción
    port: 1025       # Puerto de MailHog
    
importacion:
  chunk-size: 1000
  temp-directory: ./temp-uploads
```

### Variables de Entorno (Producción)

```bash
export DB_URL=jdbc:postgresql://production-db:5432/sueldos_db
export DB_USER=prod_user
export DB_PASSWORD=secure_password
export REDIS_HOST=production-redis
export RABBITMQ_HOST=production-rabbitmq
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USER=noreply@empresa.com
export SMTP_PASSWORD=app_password
```

---

## 📖 Uso

### 1. Preparar Archivo Excel

Tu archivo debe tener estas columnas en el siguiente orden:

| Columna | Tipo | Ejemplo | Validación |
|---------|------|---------|------------|
| NumeroEmpleado | Texto | EMP00001 | Requerido, max 50 chars |
| NombreCompleto | Texto | Juan Pérez | Requerido, max 200 chars |
| Puesto | Texto | Gerente | Requerido, max 100 chars |
| SalarioBase | Numérico | 50000.00 | Requerido, > 0 |
| Bonos | Numérico | 5000.00 | Opcional, >= 0 |
| Deducciones | Numérico | 7500.00 | Opcional, >= 0 |
| SalarioNeto | Numérico | 47500.00 | Requerido, = Base + Bonos - Deducciones |
| PeriodoPago | Texto | 2025-01 | Requerido, formato YYYY-MM |
| FechaPago | Fecha | 2025-01-15 | Requerido, formato YYYY-MM-DD |

**Fórmula Crítica**: `SalarioNeto = SalarioBase + Bonos - Deducciones`

### 2. Subir Archivo

1. Accede a `http://localhost:4200`
2. Arrastra tu archivo Excel o haz clic para seleccionar
3. Ingresa tu email para recibir notificaciones
4. Haz clic en "Subir e Importar"

### 3. Monitorear Progreso

- **En tiempo real**: La UI se actualiza automáticamente vía WebSocket
- **Manualmente**: Consulta el endpoint `/api/importacion/status/{jobId}`
- **RabbitMQ**: Visualiza las colas en `http://localhost:15672`

### 4. Revisar Resultados

- **Email**: Recibirás un correo con el resumen
- **Dashboard**: Consulta el historial en la sección "Importaciones"
- **Errores**: Descarga el reporte de registros fallidos

---

## 🌐 API Endpoints

### Importación

#### POST `/api/importacion/upload`
Sube un archivo Excel para procesamiento.

**Request:**
```http
POST /api/importacion/upload
Content-Type: multipart/form-data

file: [archivo.xlsx]
userEmail: usuario@empresa.com
```

**Response:**
```json
{
  "status": 202,
  "statusMessage": "Accepted",
  "success": true,
  "data": {
    "jobId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "message": "Importación iniciada exitosamente",
    "statusUrl": "/api/importacion/status/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  },
  "message": "Importación iniciada exitosamente. Recibirás un email al finalizar.",
  "timestamp": "2025-10-23T14:30:00"
}
```

#### GET `/api/importacion/status/{jobId}`
Consulta el estado de un job.

**Response:**
```json
{
  "status": 200,
  "success": true,
  "data": {
    "jobId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "filename": "sueldos_enero_2025.xlsx",
    "status": "EN_PROCESO",
    "totalRegistros": 10000,
    "procesados": 5000,
    "exitosos": 4950,
    "errores": 50,
    "progreso": 50.0,
    "createdAt": "2025-10-23T14:30:00",
    "completedAt": null
  }
}
```

#### GET `/api/importacion/jobs?userEmail={email}`
Lista todos los jobs, opcionalmente filtrados por email.

**Response:**
```json
{
  "status": 200,
  "success": true,
  "data": [
    {
      "jobId": "...",
      "filename": "sueldos.xlsx",
      "status": "COMPLETADO",
      "totalRegistros": 1000,
      "exitosos": 995,
      "errores": 5,
      "progreso": 100.0,
      "createdAt": "2025-10-23T14:00:00",
      "completedAt": "2025-10-23T14:05:30"
    }
  ],
  "message": "Se encontraron 1 importaciones"
}
```

#### GET `/api/importacion/errors/{jobId}`
Obtiene los errores de un job específico.

**Response:**
```json
{
  "status": 200,
  "success": true,
  "data": [
    {
      "id": 1,
      "jobId": "a1b2c3d4-...",
      "rowNumber": 45,
      "numeroEmpleado": "EMP00045",
      "errorMessage": "Salario neto no coincide con la fórmula",
      "rawData": {
        "numeroEmpleado": "EMP00045",
        "salarioBase": 50000,
        "bonos": 5000,
        "deducciones": 7500,
        "salarioNeto": 50000
      },
      "createdAt": "2025-10-23T14:31:15"
    }
  ],
  "message": "Se encontraron 1 errores"
}
```

#### DELETE `/api/importacion/job/{jobId}`
Elimina un job y sus errores asociados.

**Response:**
```json
{
  "status": 200,
  "success": true,
  "data": null,
  "message": "Job eliminado exitosamente"
}
```

#### PUT `/api/importacion/job/{jobId}/cancel`
Cancela un job en proceso.

**Response:**
```json
{
  "status": 200,
  "success": true,
  "data": null,
  "message": "Job cancelado exitosamente"
}
```

#### GET `/api/importacion/stats`
Obtiene estadísticas generales.

**Response:**
```json
{
  "status": 200,
  "success": true,
  "data": {
    "total": 150,
    "completados": 120,
    "enProceso": 5,
    "errores": 25
  },
  "message": "Estadísticas obtenidas"
}
```

### Sueldos

#### GET `/api/sueldos?page=0&size=20`
Lista todos los sueldos con paginación.

#### GET `/api/sueldos/empleado/{numeroEmpleado}`
Busca sueldos por número de empleado.

#### GET `/api/sueldos/periodo/{periodo}`
Busca sueldos por periodo (ej: 2025-01).

---

## 📁 Estructura del Proyecto

```
bulk-salary-import-system/
│
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/
│   │   └── com/empresa/sueldos/
│   │       ├── SueldoImportApplication.java
│   │       ├── config/               # Configuraciones
│   │       │   ├── AsyncConfig.java
│   │       │   ├── RedisConfig.java
│   │       │   ├── RabbitMQConfig.java
│   │       │   ├── WebSocketConfig.java
│   │       │   └── CorsConfig.java
│   │       ├── model/                # Entidades JPA
│   │       │   ├── Sueldo.java
│   │       │   ├── ImportacionJob.java
│   │       │   └── ImportacionError.java
│   │       ├── dto/                  # Data Transfer Objects
│   │       │   ├── SueldoDTO.java
│   │       │   ├── ApiResponse.java
│   │       │   ├── JobStatusDTO.java
│   │       │   ├── ChunkMessage.java
│   │       │   └── ProgressUpdate.java
│   │       ├── repository/           # Repositorios JPA
│   │       │   ├── SueldoRepository.java
│   │       │   ├── ImportacionJobRepository.java
│   │       │   └── ImportacionErrorRepository.java
│   │       ├── service/              # Lógica de negocio
│   │       │   ├── ImportacionService.java
│   │       │   ├── ExcelService.java
│   │       │   ├── EmailService.java
│   │       │   └── ValidacionService.java
│   │       ├── consumer/             # RabbitMQ Consumers
│   │       │   └── ImportacionConsumer.java
│   │       ├── controller/           # REST Controllers
│   │       │   ├── ImportacionController.java
│   │       │   └── SueldoController.java
│   │       └── exception/            # Manejo de excepciones
│   │           └── GlobalExceptionHandler.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── static/
│   │   └── templates/
│   │       └── email/
│   │           ├── importacion-completa.html
│   │           └── importacion-error.html
│   └── pom.xml
│
├── frontend/                         # Angular Frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/
│   │   │   │   ├── upload/
│   │   │   │   ├── job-status/
│   │   │   │   ├── jobs-list/
│   │   │   │   └── job-detail/
│   │   │   ├── services/
│   │   │   │   ├── importacion.service.ts
│   │   │   │   └── websocket.service.ts
│   │   │   ├── models/
│   │   │   │   ├── api-response.ts
│   │   │   │   ├── job-status.ts
│   │   │   │   └── importacion-response.ts
│   │   │   ├── app.component.ts
│   │   │   └── app.routes.ts
│   │   └── assets/
│   └── package.json
│
├── docker-compose.yml                # Infraestructura Docker
├── init-db.sql                       # Script inicial de BD
├── .gitignore
└── README.md
```

---

## 📚 Librerías y su Uso

### Spring Boot Starters

#### `spring-boot-starter-web`
- **Uso**: API REST, controllers, HTTP
- **Por qué**: Expone endpoints para el frontend Angular

#### `spring-boot-starter-data-jpa`
- **Uso**: ORM, repositories, acceso a base de datos
- **Por qué**: Simplifica el CRUD de entidades Sueldo, ImportacionJob, etc.

#### `spring-boot-starter-data-redis`
- **Uso**: Cache, almacenamiento temporal de estado
- **Por qué**: Guarda el progreso de los jobs para consulta rápida

#### `spring-boot-starter-amqp`
- **Uso**: RabbitMQ, mensajería asíncrona
- **Por qué**: Distribuye chunks a múltiples workers para procesamiento paralelo

#### `spring-boot-starter-mail`
- **Uso**: Envío de emails
- **Por qué**: Notifica al usuario cuando la importación finaliza

#### `spring-boot-starter-thymeleaf`
- **Uso**: Templates HTML para emails
- **Por qué**: Genera emails con formato profesional

#### `spring-boot-starter-websocket`
- **Uso**: Comunicación bidireccional en tiempo real
- **Por qué**: Envía actualizaciones de progreso al frontend sin polling

#### `spring-boot-starter-validation`
- **Uso**: Validación de DTOs con anotaciones
- **Por qué**: Valida datos de entrada (email, campos requeridos)

#### `spring-boot-starter-actuator`
- **Uso**: Health checks, métricas
- **Por qué**: Monitoreo del estado de la aplicación

#### `spring-boot-starter-aop`
- **Uso**: Aspect-Oriented Programming
- **Por qué**: Necesario para las anotaciones de Resilience4j (@Retry, @CircuitBreaker)

### Bases de Datos

#### `postgresql` (Driver JDBC)
- **Uso**: Conector para PostgreSQL
- **Por qué**: Base de datos principal para almacenar sueldos y jobs

### Excel Processing

#### `poi-ooxml` (Apache POI)
- **Uso**: Lectura de archivos Excel (.xlsx)
- **Por qué**: Parsea el archivo Excel y extrae los datos de sueldos

### Resilience

#### `resilience4j-spring-boot3`
- **Uso**: Circuit breaker, retry, bulkhead
- **Por qué**: Maneja fallos temporales con reintentos y previene cascadas de errores

#### `resilience4j-annotations`
- **Uso**: Anotaciones @Retry, @CircuitBreaker
- **Por qué**: Sintaxis declarativa para aplicar patrones de resiliencia

### Utilidades

#### `lombok`
- **Uso**: Reduce boilerplate (getters, setters, builders)
- **Por qué**: Código más limpio con @Data, @Builder, @Slf4j

#### `jackson-databind` + `jackson-datatype-jsr310`
- **Uso**: Serialización/deserialización JSON
- **Por qué**: Convierte objetos Java a JSON y maneja LocalDateTime

### Frontend

#### `@stomp/stompjs`
- **Uso**: Cliente WebSocket con protocolo STOMP
- **Por qué**: Recibe actualizaciones en tiempo real del backend

#### `sockjs-client`
- **Uso**: Fallback para WebSocket
- **Por qué**: Compatibilidad con navegadores que no soportan WebSocket nativo

#### `@angular/material`
- **Uso**: Componentes UI Material Design
- **Por qué**: Interfaz moderna y profesional (opcional)

#### `bootstrap`
- **Uso**: Framework CSS
- **Por qué**: Diseño responsive y componentes pre-construidos

---


### URL Final:
```
https://github.com/urtaav/bulk-salary-import-system
```

---

## 🤝 Contribución

1. Fork el proyecto
2. Crea tu rama de feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -m 'Agrega nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

---

## 📝 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

---

## 👥 Autores

- **urtaav** - *Desarrollo Inicial* - https://github.com/urtaav

---

## 🙏 Agradecimientos

- Spring Boot Community
- Apache POI Project
- RabbitMQ Team
- Angular Team

---


**Desarrollado con ❤️ usando Spring Boot y Angular**
