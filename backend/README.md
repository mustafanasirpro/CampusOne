# CampusOne Backend

Spring Boot foundation for the CampusOne university community platform.

## Current scope

The backend currently contains the Phase 0 foundation and Phase 1 core domain
model:

- Users, roles, universities, departments, courses, and student profiles
- PostgreSQL schema managed by Flyway
- Repository interfaces, response DTOs, and manual mappers
- Temporary permit-all security and one public health endpoint

Authentication and CampusOne business modules are intentionally not implemented
yet.

## Requirements

- Java 21
- Maven 3.9 or newer
- PostgreSQL

## Environment variables

The application reads its PostgreSQL connection exclusively from:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

For local development, copy the names from `.env.example` into your shell or IDE
run configuration. Do not commit real credentials.

PowerShell example:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/campusone"
$env:DB_USERNAME = "campusone"
$env:DB_PASSWORD = "<your-local-password>"
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verification

```powershell
mvn clean verify
```

## Available URLs

- Health: `GET http://localhost:8080/api/v1/health`
- OpenAPI JSON: `http://localhost:8080/api/v1/openapi`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui`
