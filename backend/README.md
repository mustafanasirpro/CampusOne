# CampusOne Backend

Spring Boot foundation for the CampusOne university community platform.

## Phase 0 scope

This phase contains application configuration, empty domain packages, temporary
permit-all security, OpenAPI configuration, and one public health endpoint. It
does not contain authentication, business modules, entities, repositories, or
application database migrations.

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
