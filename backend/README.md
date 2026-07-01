# CampusOne Backend

Spring Boot foundation for the CampusOne university community platform.

## Current scope

The backend currently contains the Phase 0 foundation, Phase 1 core domain
model, and Phase 2 authentication MVP:

- Users, roles, universities, departments, courses, and student profiles
- PostgreSQL schema managed by Flyway
- Repository interfaces, response DTOs, and manual mappers
- BCrypt password storage with the CampusOne password policy
- Stateless, 15-minute HS256 JWT access tokens
- Registration, login, and authenticated current-user endpoints
- Consistent validation, authentication, authorization, and API error responses

Refresh tokens, logout, email verification, password recovery, and CampusOne
business modules are intentionally not implemented yet.

## Requirements

- Java 21
- Maven 3.9 or newer
- PostgreSQL

## Environment variables

The application reads its PostgreSQL connection exclusively from:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

JWT signing uses:

- `JWT_SECRET`: Base64-encoded secret containing at least 32 random bytes
- `JWT_ISSUER`: optional; defaults to `campusone-backend`
- `JWT_ACCESS_TOKEN_TTL`: optional; defaults to `15m`

For local development, copy the names from `.env.example` into your shell or IDE
run configuration. Do not commit real credentials.

PowerShell example:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/campusone"
$env:DB_USERNAME = "campusone"
$env:DB_PASSWORD = "<your-local-password>"
$env:JWT_SECRET = "<base64-encoded-32-byte-or-longer-secret>"
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verification

```powershell
mvn clean verify
```

## Available URLs

- Health: `GET http://localhost:8080/api/v1/health`
- Register: `POST http://localhost:8080/api/v1/auth/register`
- Login: `POST http://localhost:8080/api/v1/auth/login`
- Current user: `GET http://localhost:8080/api/v1/users/me`
- OpenAPI JSON: `http://localhost:8080/api/v1/openapi`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui`
