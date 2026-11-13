# Restricted File Sharing System — Backend

A production-style REST API for secure file sharing built with **Spring Boot 3.4.2** and **Java 21**. Features JWT authentication, email verification, granular per-user file access control, and transactional upload/revoke operations.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-brightgreen)
![H2](https://img.shields.io/badge/H2-Database-blue)
![MySQL](https://img.shields.io/badge/MySQL-Supported-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)


## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Architecture](#-architecture)
- [Data Model](#-data-model)
- [API Endpoints](#-api-endpoints)
- [Getting Started](#-getting-started)
- [Project Structure](#-project-structure)
- [Roadmap](#️-roadmap)
- [License](#-license)
- [Author](#-author)
- [Acknowledgments](#-acknowledgments)

---

## 📖 Overview

Restricted File Sharing System is a backend REST API that simulates a real-world secure file-sharing platform. It provides:

- A secure REST API for uploading and downloading files.
- JWT-based authentication with email verification.
- Granular per-user file access control (grant/revoke).
- Transactional file operations with soft-delete.
- UUID-based secure download tokens.
- Layered architecture with DTOs, exception handling, and validation.

The primary goal was to demonstrate production-grade Java backend development skills, including Spring Security, JPA relationships, DTO layer separation, global exception handling, transactional operations, and stateless JWT authentication.

**Note:** This is a backend-only project. The API can be consumed through Postman, Swagger (when configured), or any HTTP client.

---

## 🚀 Tech Stack

| Technology         | Version | Purpose                        |
| ------------------ | ------- | ------------------------------ |
| Java               | 21      | Programming language           |
| Spring Boot        | 3.4.2   | Application framework          |
| Spring Security    | 6.x     | Authentication & authorization |
| Spring Data JPA    | 3.x     | Data access                    |
| Hibernate          | 6.x     | ORM                            |
| H2                 | Latest  | Development & testing database |
| MySQL              | 8.x     | Relational database support    |
| Gradle             | 8.x     | Build tool                     |
| Lombok             | Latest  | Boilerplate reduction          |
| JJWT               | 0.12.6  | JWT implementation             |
| Jakarta Validation | 3.x     | Request validation             |

---

## ✨ Features

### 🔐 Authentication & Authorization

- JWT-based stateless authentication.
- BCrypt password hashing.
- Email verification flow — users must verify their email before performing file operations.
- Verification token expiration and cooldown.
- Method-level security on file endpoints.
- Protected endpoints using Spring Security.

### 📁 File Management

- Multipart file upload.
- MIME-type whitelist validation.
- Configurable maximum file size.
- Granular access control — share files with specific verified users.
- Grant and revoke file access independently.
- Soft delete — files are marked `active=false` instead of being permanently deleted.
- UUID-based download tokens to prevent predictable download URLs.

### 🛡️ Engineering Quality

- Global exception handling using `@RestControllerAdvice`.
- Request validation with Jakarta Bean Validation.
- DTO layer — entities never leak directly to API responses.
- Consistent API responses using `ApiResponse<T>`.
- Scheduled cleanup of expired verification tokens.
- Transactional business operations using `@Transactional`.
- Separation of controller, service, and repository layers.

---

## 🏗️ Architecture

### Layered Design

The application follows a layered architecture to maintain separation of concerns, security, and maintainability.

```text
┌──────────────────────────────────────────────────────────┐
│                    HTTP Request                          │
└─────────────────────────┬────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────┐
│  JwtFilter — validates JWT, sets SecurityContext         │
└─────────────────────────┬────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────┐
│  @RestController — HTTP concerns, authentication         │
└─────────────────────────┬────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────┐
│  @Service — business logic, @Transactional boundaries    │
└─────────────────────────┬────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────┐
│  @Repository (Spring Data JPA) — data access             │
└─────────────────────────┬────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────┐
│  Hibernate ORM ──▶ H2 / MySQL                            │
└──────────────────────────────────────────────────────────┘
```

### Package Structure

```text
config/         → SecurityConfig, JwtFilter, JwtUtil, WebConfig
controller/     → REST endpoints
service/        → Business logic and file operations
repository/     → Spring Data JPA interfaces
model/          → JPA entities
dto/            → Request and response contracts
exception/      → Global handler and custom exceptions
validation/     → File validation
```

---

## 📦 Data Model

### Entity Relationships

```text
User ──(uploadedBy)──▶ FileEntity
                            │
                            │ 1..N
                            ▼
                       FileAccess
                            │
                            │ N..1
                            ▼
                           User

User ──▶ VerificationToken (1..N)
```

**4 Database Tables:**

`users`, `files`, `file_access`, `verification_token`

### Key Entities

| Entity            | Purpose                                          |
| ----------------- | ------------------------------------------------ |
| User              | Application users with email verification status |
| FileEntity        | Uploaded files with metadata and download tokens |
| FileAccess        | Controls which users can access specific files   |
| VerificationToken | Stores email verification tokens with expiration |

---

## 🔌 API Endpoints

**Base URL:** `http://localhost:8080`

### Authentication (Public)

| Method | Endpoint                               | Description                      |
| ------ | -------------------------------------- | -------------------------------- |
| POST   | `/api/auth/register`                   | Register with email and password |
| GET    | `/api/auth/verify?token=`              | Verify email address             |
| POST   | `/api/auth/login`                      | Login and receive JWT            |
| POST   | `/api/auth/resend-verification?email=` | Resend verification email        |

### Files (Authenticated)

| Method | Endpoint                                   | Description               |
| ------ | ------------------------------------------ | ------------------------- |
| POST   | `/api/files/upload`                        | Upload a file (multipart) |
| GET    | `/api/files/download/{token}`              | Download a file           |
| GET    | `/api/files/my-files`                      | List accessible files     |
| POST   | `/api/files/{id}/grant-access?userEmail=`  | Grant access to a user    |
| POST   | `/api/files/{id}/revoke-access?userEmail=` | Revoke user access        |
| DELETE | `/api/files/{id}`                          | Soft-delete a file        |

**Authentication:** Include the following header when accessing protected endpoints:

```http
Authorization: Bearer <your-jwt-token>
```

---

## 🚦 Getting Started

### Prerequisites

- Java 21 — [Adoptium JDK 21](https://adoptium.net/)
- Git
- Gradle 8+ (or use the included Gradle wrapper)

### 1. Clone the Repository

```bash
git clone https://github.com/rsrabbey17/Restricted-File-Sharing-System-Application.git

cd Restricted-File-Sharing-System-Application
```

### 2. Configure application.properties

Create or edit:

`src/main/resources/application.properties`

```properties
# JWT Configuration
jwt.secret=your-256-bit-secret-key-here-change-in-production-at-least-32-bytes
jwt.expiration=86400000

# File Storage
app.file.max-size-mb=10
app.file.upload-dir=./uploads

# Email Verification
app.verification.cooldown.minutes=2

# Multipart Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true
```

**Important:** Replace the JWT secret with a secure secret of at least 256 bits before using the application in production. Configure your email provider if required for real email delivery.

### 3. Run the Application

**Windows:**

```bash
gradlew.bat bootRun
```

**Linux / macOS:**

```bash
./gradlew bootRun
```

The server starts at:

`http://localhost:8080`

### 4. Test the Authentication and File-Sharing Flow

Follow these steps to test the main functionality:

1. **Register** → `POST /api/auth/register`
2. **Verify Email** → `GET /api/auth/verify?token=...`
3. **Login** → `POST /api/auth/login`
4. **Save JWT** → Copy the access token from the login response.
5. **Upload File** → `POST /api/files/upload`
6. **Grant Access** → Share the uploaded file with another verified user.
7. **Download File** → Use the generated download token.
8. **Revoke Access** → Remove the recipient's permission.

During development, copy the email verification link from the application console if console-based email delivery is configured.

### 5. Access Points

| URL                                       | Purpose                                |
| ----------------------------------------- | -------------------------------------- |
| `http://localhost:8080/h2-console`        | H2 database console (development only) |
| `http://localhost:8080/api/auth/register` | Registration endpoint                  |

---

## 📁 Project Structure

```text
Restricted-File-Sharing-System-Application/
│
├── src/
│   ├── main/
│   │   ├── java/com/restrictedfileshare/
│   │   │   ├── RestrictedFileSharingSystemApplication.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── JwtFilter.java
│   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   └── FileController.java
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── FileUploadResponse.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   └── RegisterRequest.java
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── UnauthorizedException.java
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── FileAccess.java
│   │   │   │   ├── FileEntity.java
│   │   │   │   ├── User.java
│   │   │   │   └── VerificationToken.java
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── FileAccessRepository.java
│   │   │   │   ├── FileRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── VerificationTokenRepository.java
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   ├── FileService.java
│   │   │   │   └── TokenService.java
│   │   │   │
│   │   │   └── validation/
│   │   │       └── FileValidator.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/com/restrictedfileshare/
│
├── .gitignore
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
```

---

## 🗺️ Roadmap

### ✅ Completed

- [x] JWT authentication with BCrypt password hashing
- [x] Email verification flow
- [x] Verification token expiration and cooldown
- [x] File upload with MIME-type validation
- [x] Configurable file size restrictions
- [x] Granular per-user file access control
- [x] Grant and revoke file permissions
- [x] Secure UUID-based download tokens
- [x] Soft-delete functionality
- [x] Global exception handling
- [x] Layered architecture with DTOs
- [x] Transactional file operations
- [x] Scheduled verification token cleanup

### 🚧 Planned

- [ ] Integration tests (unit, controller, and full-flow coverage)
- [ ] Swagger/OpenAPI documentation
- [ ] Docker and docker-compose
- [ ] GitHub Actions CI pipeline
- [ ] Real SMTP email provider integration
- [ ] File encryption at rest
- [ ] Rate limiting on authentication endpoints
- [ ] Admin dashboard for audit logs
- [ ] Deployment to Render / Railway

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Redowan Sultan Rabbey**

- GitHub: [@rsrabbey17](https://github.com/rsrabbey17)
- Repository: [Restricted-File-Sharing-System-Application](https://github.com/rsrabbey17/Restricted-File-Sharing-System-Application)

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Hibernate ORM](https://hibernate.org/)
- [JWT.io](https://jwt.io/)
- [Baeldung](https://www.baeldung.com/)
