# Smart Healthcare System

A comprehensive microservices-based healthcare management platform designed to streamline medical operations, patient care coordination, and administrative workflows.

## 🏥 Overview

The Smart Healthcare System provides a scalable, cloud-ready architecture for managing appointments, patient records, doctor profiles, and user authentication. Built using Spring Boot microservices with service discovery, centralized configuration, event-driven communication, and automated email notifications.

## ✨ Key Features

### Core Functionality
- **User Management** — Secure registration, authentication, and JWT-based authorization
- **Role System** — Request-based role promotion (USER → PATIENT / DOCTOR / ADMIN) with admin approval workflow
- **Patient Management** — Patient profiles, appointment history, health vitals tracking
- **Doctor Management** — Doctor profiles, specializations, availability scheduling, leave management
- **Appointment System** — Booking, cancellation, completion with full rollback support
- **Notifications** — Event-driven email notifications via Kafka (booking, cancellation, prescription, daily schedule)

### Technical Features
- Microservices architecture with independent scaling
- Service discovery and registration (Eureka)
- Centralized configuration management (Spring Cloud Config)
- API Gateway with JWT validation and correlation ID propagation
- Event-driven messaging (Kafka) — fire-and-forget for notifications
- Synchronous inter-service communication (OpenFeign) with Circuit Breaker + Retry
- Atomic booking and cancellation flows with explicit rollback
- MongoDB auditing (`@CreatedDate`, `@LastModifiedDate`, `@LastModifiedBy`)
- Docker containerization with log volume mounts
- Health monitoring via Actuator endpoints

## 🏗️ Architecture

### Microservices (9 Services)

**Infrastructure Services:**
- **Config Server** (8888) — Centralized configuration management
- **Eureka Server** (8761) — Service discovery and registration
- **API Gateway** (8080) — Single entry point, JWT validation, header propagation

**Business Services:**
- **User Service** (8010) — Registration, authentication, JWT generation, role requests
- **Admin Service** (8011) — Role request approval/decline workflow
- **Doctor Service** (8012) — Doctor profiles, availability, leave management, daily schedule
- **Patient Service** (8013) — Patient profiles, appointment booking and cancellation
- **Appointment Service** (8014) — Appointment persistence, completion, prescription
- **Notification Service** (8015) — Kafka consumer, email dispatch via Thymeleaf + SMTP

**Messaging Infrastructure:**
- **Kafka** (9092) — Message broker
- **Zookeeper** (2181) — Kafka coordination
- **Kafka UI** (9090) — Kafka monitoring interface

### Database Architecture
- **MySQL** — User Service (authentication and user data)
- **MongoDB** — Doctor, Patient, Appointment, Admin, Notification services

### Communication Patterns
- **Synchronous** — REST via OpenFeign with Circuit Breaker + Retry (Resilience4j)
- **Asynchronous** — Kafka for all notification events (fire-and-forget)
- **Service Discovery** — Eureka-based dynamic service location

### Kafka Topics

| Topic | Publisher | Consumer |
|---|---|---|
| `welcome-notification` | userService | notificationService |
| `role-request` | userService | adminService |
| `role-approved` | adminService | notificationService |
| `role-declined` | adminService | notificationService |
| `appointment-booked` | patientService | notificationService |
| `appointment-cancelled-notification` | patientService, doctorService | notificationService |
| `appointment-completed` | appointmentService | patientService, notificationService |
| `send-email-appointment` | patientService | notificationService |
| `doctor-daily-schedule` | doctorService | notificationService |

## 🛠️ Technology Stack

### Backend
- **Java 17**
- **Spring Boot** 3.5.5 / 4.0.1
- **Spring Cloud** 2025.0.0
  - Spring Cloud Config
  - Spring Cloud Netflix Eureka
  - Spring Cloud Gateway
  - Spring Cloud OpenFeign
- **Resilience4j** — Circuit Breaker + Retry
- **Spring Security** — Authentication and authorization
- **Spring Data JPA** — MySQL (User Service)
- **Spring Data MongoDB** — All other services
- **Spring Kafka** — Event streaming
- **Thymeleaf** — Email templates
- **Flying Saucer + OpenPDF** — PDF generation for prescriptions

### Security
- **JWT** — Token-based authentication (email + role claims)
- **BCrypt** — Password hashing
- **Role-Based Access Control** — USER, PATIENT, DOCTOR, ADMIN
- **Correlation IDs** — Propagated across HTTP and Kafka for distributed tracing

### Databases
- **MySQL 8.0** — Relational (User Service)
- **MongoDB 7.0** — NoSQL document store (all other services)

### Messaging
- **Apache Kafka** (Confluent 7.5.0)
- **Apache Zookeeper** (Confluent 7.5.0)

### Build & Deployment
- **Maven** — Build automation
- **Docker + Docker Compose** — Containerization and orchestration
- **Lombok** — Boilerplate reduction

## 📋 Prerequisites

### Required Software
- **JDK 17**
- **Maven 3.6+** (or use included wrapper)
- **Docker Desktop** with Docker Compose
- **MySQL 8.0** running on `localhost:3306`
- **MongoDB 7.0** running on `localhost:27017`

### Database Setup
```sql
-- MySQL
CREATE DATABASE userservice;
```
MongoDB database `smart-healthcare-service` is created automatically.

### Environment Variables
Create `Vault.env` in the root directory:
```env
DATABASE_USERNAME=root
DATABASE_PASS=root
SECRET_KEY=your-secret-key-here
EXPIRATION=43200000
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
```
> `Vault.env` is in `.gitignore` — never commit real credentials.

## 🚀 Quick Start

### 1. Clone Repository
```bash
git clone <repository-url>
cd smart-healthcare-system
```

### 2. Build All Services
```bash
build-infrastructure.bat
```
Builds all 9 services and runs tests (~5-10 minutes).

### 3. Start Services
```bash
docker compose up -d
```

### 4. Verify
- **Eureka Dashboard**: http://localhost:8761
- **API Gateway Health**: http://localhost:8080/actuator/health
- **Config Server Health**: http://localhost:8888/actuator/health
- **Kafka UI**: http://localhost:9090

All services register with Eureka within 1-2 minutes.

## 📡 API Access

### Gateway Routes
All requests go through the Gateway (`http://localhost:8080`):

```
User Service:         /api/user-service/**
Admin Service:        /api/admin-service/**
Doctor Service:       /api/doctor-service/**
Patient Service:      /api/patient-service/**
Appointment Service:  /api/appointment-service/**
Notification Service: /api/notification-service/**
```

### Authentication
1. **Register**: `POST /api/user-service/open/newUser`
2. **Login**: `POST /api/user-service/open/login`
3. **Use token**: `Authorization: Bearer <token>`

### Endpoint Security
- `/open/*` — Public, no authentication required
- `/secure/*` — JWT required

## 🔧 How It Works

### Service Startup Order
```
1. Config Server  → provides configuration to all services
2. Eureka Server  → service registry
3. Gateway        → routing + JWT validation
4. Kafka + Zookeeper → messaging infrastructure
5. Business services → register with Eureka, connect to DBs
```

### Request Flow
```
Client → Gateway (JWT validation + header injection)
       → Eureka (service discovery)
       → Target Service → Database
       → Response
```

### Authentication Flow
```
1. POST /api/user-service/open/login
2. User Service validates credentials against MySQL
3. JWT generated with email + role claims
4. Client sends JWT in Authorization header
5. Gateway validates JWT, injects X-User-Email + X-User-Role headers
6. Downstream services trust these headers (no re-validation)
```

### Role Promotion Flow
```
1. User requests role (PATIENT / DOCTOR / ADMIN) via user-service
2. Request published to Kafka topic: role-request
3. Admin Service consumes, saves request as PENDING
4. Admin approves/declines via admin-service
5. On approval: user role updated, profile created (doctor/patient)
6. Kafka event published: role-approved / role-declined
7. Notification Service sends email to user
```

## 🗂️ Project Structure

```
smart-healthcare-system/
├── configServer/           # Spring Cloud Config Server
├── eurekaServer/           # Netflix Eureka Server
├── gateway/                # Spring Cloud Gateway + JWT filter
├── userService/            # Auth, registration, role requests
├── adminService/           # Role approval workflow
├── doctorService/          # Doctor profiles, availability, leave
├── patientService/         # Patient profiles, booking, cancellation
├── appointmentService/     # Appointment persistence + completion
├── notificationService/    # Kafka consumers + email dispatch
├── logs/                   # Per-service log directories
├── docker-compose.yml
├── build-infrastructure.bat
├── Vault.env               # Secrets (gitignored)
├── README.md
├── INFRASTRUCTURE.md
└── FLOW.md                 # End-to-end flow documentation
```

Each service:
```
<serviceName>/
├── src/main/java/com/project/<serviceName>/
│   ├── Controller/     # REST endpoints + GlobalExceptionHandler
│   ├── Entity/         # DB entities (JPA / MongoDB documents)
│   ├── Model/          # DTOs, enums, request/response models
│   ├── Repository/     # Spring Data repositories
│   ├── Service/        # Business logic + Kafka listeners
│   ├── Exceptions/     # Custom exceptions
│   ├── Filters/        # CorrelationFilter, FeignInterceptor
│   ├── RESTCalls/      # Feign clients
│   └── Utility/        # UtilityFunctions, LogUtil, JWTUtil
├── src/main/resources/
│   └── application.yml
├── Dockerfile
└── pom.xml
```

## 🔍 Service Details

### User Service (8010) — MySQL
- Registration, login, JWT generation
- Role request submission (PATIENT / DOCTOR / ADMIN)
- Password encryption (BCrypt)
- Roles: USER, PATIENT, DOCTOR, ADMIN

### Admin Service (8011) — MongoDB
- Consumes `role-request` Kafka topic
- Approve / decline role requests
- On approval: calls user-service (role change) + doctor/patient-service (profile creation)
- Publishes `role-approved` / `role-declined`

### Doctor Service (8012) — MongoDB
- Doctor profile and specialization management
- Booking schedule (31-day rolling window, refreshed daily at midnight)
- Leave management with per-appointment cancellation and patient notification
- Daily schedule notification published at 00:05 via scheduler

### Patient Service (8013) — MongoDB
- Patient profile management
- Appointment booking (atomic: appointment + patient list + doctor schedule)
- Appointment cancellation (markCancelled + doctor schedule update)
- Health vitals updated on appointment completion via Kafka
- Prescription email trigger

### Appointment Service (8014) — MongoDB
- Appointment persistence (save, markCancelled, restore, delete, complete)
- Publishes `appointment-completed` on visit completion
- Internal-only endpoints (called by patient/doctor services)

### Notification Service (8015)
- Consumes all 9 Kafka notification topics
- Sends HTML emails via JavaMailSender + Thymeleaf templates
- Prescription PDF generation (Flying Saucer + OpenPDF)
- No database, stateless

## 🧪 Development

### Running Locally (Without Docker)
```bash
cd configServer && mvnw spring-boot:run
cd eurekaServer && mvnw spring-boot:run
cd gateway && mvnw spring-boot:run
# then business services
```

### Building Individual Service
```bash
cd <serviceName>
mvnw clean package
```

### Logs
```bash
docker compose logs -f                    # all services
docker compose logs -f user-service       # specific service
# or check ./logs/<service-name>/ directory
```

### Rebuild After Changes
```bash
cd <serviceName>
mvnw clean package
cd ..
docker compose up -d --build <service-name>
```

## 📊 Monitoring

| Endpoint | URL |
|---|---|
| Eureka Dashboard | http://localhost:8761 |
| Kafka UI | http://localhost:9090 |
| Config Server Health | http://localhost:8888/actuator/health |
| Gateway Health | http://localhost:8080/actuator/health |
| Any Service Health | http://localhost:<port>/api/<service>/open/health |

## 🔐 Security

- JWT secret and expiration in `Vault.env`
- Token expiration: 12 hours (configurable via `EXPIRATION`)
- Claims: `sub` (email), `role`
- Gateway injects `X-User-Email` and `X-User-Role` — downstream services trust these
- All inter-service calls use ADMIN role impersonation (internal trust model)
- Passwords hashed with BCrypt

## 🐛 Troubleshooting

| Problem | Solution |
|---|---|
| Service won't start | Check `docker compose logs <service>`, verify Vault.env exists |
| Not registering with Eureka | Wait 1-2 min, check `docker compose logs <service>` |
| DB connection failed | Verify MySQL/MongoDB running, check `host.docker.internal` resolution |
| Port conflict | `netstat -ano \| findstr :<port>`, kill conflicting process |
| Config Server not found | Ensure config-server is healthy before starting other services |
| Email not sending | Check EMAIL_USERNAME/EMAIL_PASSWORD in Vault.env, use app password for Gmail |

## 📚 Additional Documentation

- **[INFRASTRUCTURE.md](INFRASTRUCTURE.md)** — Docker setup, environment variables, networking
- **[FLOW.md](FLOW.md)** — End-to-end flow documentation for every feature
