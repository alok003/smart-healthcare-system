# Smart Healthcare System — Infrastructure Documentation

Complete guide for infrastructure setup, deployment, configuration, and operations.

---

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [Service Port Mapping](#service-port-mapping)
- [Docker Setup](#docker-setup)
- [Database Configuration](#database-configuration)
- [Environment Variables](#environment-variables)
- [Build & Deployment](#build--deployment)
- [Networking](#networking)
- [Log Management](#log-management)
- [Schedulers](#schedulers)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                      Client / Browser                         │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                   API Gateway (8080)                          │
│         JWT Validation · Routing · Correlation IDs           │
└──────┬──────────────────────────────────────────────┬────────┘
       │                                              │
       ▼                                              ▼
┌─────────────┐                             ┌──────────────────┐
│Config Server│                             │  Eureka Server   │
│   (8888)    │                             │     (8761)       │
└─────────────┘                             └──────────────────┘
                                                     │
                    ┌────────────────────────────────┤
                    │                                │
          ┌─────────▼──────────┐          ┌─────────▼──────────┐
          │  Business Services │          │  Messaging Layer    │
          │  User     (8010)   │          │  Zookeeper  (2181)  │
          │  Admin    (8011)   │          │  Kafka      (9092)  │
          │  Doctor   (8012)   │          │  Kafka UI   (9090)  │
          │  Patient  (8013)   │          └────────────────────┘
          │  Appointment(8014) │
          │  Notification(8015)│
          └────────┬───────────┘
                   │
       ┌───────────┴───────────┐
       ▼                       ▼
┌─────────────┐         ┌─────────────┐
│    MySQL    │         │   MongoDB   │
│  (3306)     │         │  (27017)    │
│ userservice │         │ smart-      │
│             │         │ healthcare- │
│             │         │ service     │
└─────────────┘         └─────────────┘
```

---

## Service Port Mapping

| Service | Container Name | Port | Database |
|---|---|---|---|
| Config Server | config-server | 8888 | — |
| Eureka Server | eureka-server | 8761 | — |
| API Gateway | gateway | 8080 | — |
| User Service | user-service | 8010 | MySQL |
| Admin Service | admin-service | 8011 | MongoDB |
| Doctor Service | doctor-service | 8012 | MongoDB |
| Patient Service | patient-service | 8013 | MongoDB |
| Appointment Service | appointment-service | 8014 | MongoDB |
| Notification Service | notification-service | 8015 | — |
| Zookeeper | zookeeper | 2181 | — |
| Kafka | kafka | 9092 | — |
| Kafka UI | kafka-ui | 9090 | — |
| MySQL | — (host) | 3306 | — |
| MongoDB | — (host) | 27017 | — |

### Access URLs

**From browser:**
```
Eureka Dashboard:     http://localhost:8761
Kafka UI:             http://localhost:9090
Config Server:        http://localhost:8888/actuator/health
API Gateway:          http://localhost:8080/actuator/health
User Service:         http://localhost:8080/api/user-service/open/health
Admin Service:        http://localhost:8080/api/admin-service/open/health
Doctor Service:       http://localhost:8080/api/doctor-service/open/health
Patient Service:      http://localhost:8080/api/patient-service/open/health
Appointment Service:  http://localhost:8080/api/appointment-service/open/health
Notification Service: http://localhost:8080/api/notification-service/open/health
```

**From Docker network (container-to-container):**
```
config-server:8888
eureka-server:8761
user-service:8010
admin-service:8011
doctor-service:8012
patient-service:8013
appointment-service:8014
notification-service:8015
kafka:9092
zookeeper:2181
```

---

## Docker Setup

### Prerequisites
- Docker Desktop installed and running
- MySQL running on `localhost:3306`
- MongoDB running on `localhost:27017`
- All services built (JAR files present in `target/` folders)
- `Vault.env` file present in root directory

### Startup Order

Docker Compose enforces this dependency chain:

```
Config Server (health check)
    ↓
Eureka Server (health check)
    ↓
Gateway + Zookeeper (parallel)
    ↓
Kafka
    ↓
All Business Services + Kafka UI (parallel)
```

### Docker Commands

```bash
# Start all services
docker compose up -d

# Start infrastructure only
docker compose up -d config-server eureka-server gateway zookeeper kafka kafka-ui

# Start specific business service
docker compose up -d user-service

# View logs (all)
docker compose logs -f

# View logs (specific service)
docker compose logs -f notification-service

# Check status
docker compose ps

# Stop all
docker compose down

# Stop specific service
docker compose stop doctor-service

# Restart specific service
docker compose restart patient-service

# Rebuild and restart after code change
cd <serviceName>
mvnw clean package
cd ..
docker compose up -d --build <service-name>
```

### Memory Allocation

| Service | Memory Limit | Reservation |
|---|---|---|
| Config Server | 384M | 256M |
| Eureka Server | 384M | 256M |
| API Gateway | 512M | 384M |
| User Service | 512M | 384M |
| Admin Service | 384M | 256M |
| Doctor Service | 384M | 256M |
| Patient Service | 384M | 256M |
| Appointment Service | 384M | 256M |
| Notification Service | 384M | 256M |
| Kafka | 768M | 512M |
| Zookeeper | 256M | 192M |
| Kafka UI | 256M | 128M |

---

## Database Configuration

### MySQL — User Service

```
Host:     localhost (host.docker.internal from containers)
Port:     3306
Database: userservice
Username: ${DATABASE_USERNAME} from Vault.env
Password: ${DATABASE_PASS} from Vault.env
```

**Setup:**
```sql
CREATE DATABASE userservice;
```

**Schema:** Auto-created by Hibernate (`spring.jpa.hibernate.ddl-auto=update`)

**Table:** `users` — userId (UUID), userEmail, userPassword, userName, userAge, userRole, createdAt, updatedAt

### MongoDB — All Other Services

```
Host:     localhost (host.docker.internal from containers)
Port:     27017
Database: smart-healthcare-service
Auth:     None (development)
```

**Collections:**

| Collection | Service | Key Fields |
|---|---|---|
| `Doctors` | doctorService | email, gender, specializations, bookings, licenseNumber |
| `Patients` | patientService | email, name, dateOfBirth, gender, appointmentList, vitalsFlow |
| `Appointments` | appointmentService | patientId, doctorId, status, date, visitDetails, description |
| `Requests` | adminService | userEmail, userRole, requestStatus, doctorDto, patientDto |

**Auditing:** `@EnableMongoAuditing` active on all MongoDB services — `createdAt`, `updatedAt`, `lastModifiedBy` auto-populated.

---

## Environment Variables

### Vault.env

```env
DATABASE_USERNAME=root
DATABASE_PASS=root
SECRET_KEY=your-jwt-secret-key-min-32-chars
EXPIRATION=43200000
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
```

> For Gmail: generate an App Password at https://myaccount.google.com/apppasswords (requires 2FA enabled)

### Variable Reference

| Variable | Used By | Purpose |
|---|---|---|
| `DATABASE_USERNAME` | user-service | MySQL username |
| `DATABASE_PASS` | user-service | MySQL password |
| `SECRET_KEY` | gateway, user-service | JWT signing key |
| `EXPIRATION` | user-service | JWT expiry in milliseconds (43200000 = 12h) |
| `EMAIL_USERNAME` | notification-service | SMTP sender address |
| `EMAIL_PASSWORD` | notification-service | SMTP password / app password |

### Docker Environment Variables (set in docker-compose.yml)

| Variable | Value in Docker | Default (local) |
|---|---|---|
| `CONFIG_HOST` | `config-server` | `localhost` |
| `EUREKA_HOST` | `eureka-server` | `localhost` |
| `MYSQL_HOST` | `host.docker.internal` | `localhost` |
| `MONGO_HOST` | `host.docker.internal` | `localhost` |
| `KAFKA_HOST` | `kafka` | `localhost` |

---

## Build & Deployment

### Build All Services
```bash
build-infrastructure.bat
```

This runs `mvnw clean package` (with tests) for all 9 services in order. Build time: ~5-10 minutes.

> Infrastructure services (eurekaServer, gateway) use `-DskipTests` as they have no business logic tests.

### Build Individual Service
```bash
cd <serviceName>
mvnw clean package
cd ..
```

### Full Deployment Steps

```bash
# 1. Verify prerequisites
java -version          # must be 17
docker --version
# ensure MySQL on :3306 and MongoDB on :27017

# 2. Create Vault.env in root directory

# 3. Create MySQL database
# mysql -u root -p -e "CREATE DATABASE userservice;"

# 4. Build all services
build-infrastructure.bat

# 5. Start all services
docker compose up -d

# 6. Wait ~2 minutes, then verify
# http://localhost:8761 — all 6 business services should appear
# http://localhost:9090 — Kafka UI
```

### Rebuild Single Service After Code Change
```bash
cd userService
mvnw clean package
cd ..
docker compose up -d --build user-service
```

---

## Networking

### Docker Network: `healthcare-network`

- **Type:** Bridge
- **DNS:** Container names resolve automatically within the network
- **Host access:** `host.docker.internal` resolves to the host machine

### Host Access from Containers

Services that need MySQL or MongoDB use:
```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

This is configured for: user-service, admin-service, doctor-service, patient-service, appointment-service.

### Inter-Service Communication

All service-to-service calls go through Eureka service discovery:
- Feign clients use service names (e.g., `@FeignClient(name = "doctor-service")`)
- Eureka resolves to the actual container IP
- Resilience4j Circuit Breaker + Retry wraps all Feign calls

---

## Log Management

### Log Directories

Each service writes logs to `/app/logs` inside the container, mounted to the host:

| Service | Host Path |
|---|---|
| gateway | `./logs/gateway/` |
| user-service | `./logs/user-service/` |
| admin-service | `./logs/admin-service/` |
| doctor-service | `./logs/doctor-service/` |
| patient-service | `./logs/patient-service/` |
| appointment-service | `./logs/appointment-service/` |
| notification-service | `./logs/notification-service/` |

### Log Format

All services use structured logging with MDC:
```
action=<ACTION> status=<STATUS> identifier=<EMAIL_OR_ID> ...
```

### Correlation IDs

Every HTTP request gets a `X-Correlation-ID` header (generated at gateway if not present). This ID is:
- Propagated to all downstream Feign calls via `FeignCorrelationInterceptor`
- Included in all Kafka message headers
- Set in MDC by each Kafka listener
- Included in all log lines via MDC key `correlationId`

---

## Schedulers

Both schedulers run in **doctorService**:

| Scheduler | Cron | What it does |
|---|---|---|
| `refreshBookingSchedules` | `0 0 0 * * *` (midnight) | Removes past dates, adds new dates to maintain 31-day rolling window for all doctors |
| `sendDailyScheduleNotification` | `0 5 0 * * *` (00:05) | Publishes `doctor-daily-schedule` Kafka event for each doctor with appointments tomorrow |

The 5-minute gap ensures `refreshBookingSchedules` completes before the notification scheduler reads the updated schedule.

---

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker compose logs <service-name>

# Verify JAR exists
dir <serviceName>\target\*.jar

# Rebuild
cd <serviceName>
mvnw clean package
cd ..
docker compose up -d --build <service-name>
```

### Service Not Registering with Eureka

- Wait 1-2 minutes — Eureka registration has a heartbeat delay
- Check service logs: `docker compose logs -f <service-name>`
- Verify Config Server is healthy: http://localhost:8888/actuator/health
- Restart: `docker compose restart <service-name>`

### Database Connection Failed

```bash
# Verify host.docker.internal resolves
docker compose exec user-service ping host.docker.internal

# Check MySQL is running on host
# Check MongoDB is running on host

# Verify Vault.env credentials
```

### Kafka Issues

```bash
# Check Kafka is running
docker compose ps kafka

# View Kafka logs
docker compose logs kafka

# Check topics in Kafka UI
# http://localhost:9090
```

### Email Not Sending

- Verify `EMAIL_USERNAME` and `EMAIL_PASSWORD` in `Vault.env`
- For Gmail: use an App Password (not your account password)
- Check notification-service logs: `docker compose logs -f notification-service`

### Port Already in Use

```bash
# Find process (Windows)
netstat -ano | findstr :<port>

# Kill process
taskkill /PID <pid> /F
```

### Diagnostic Commands

```bash
# All container status
docker compose ps

# Resource usage
docker stats

# Network connectivity
docker compose exec user-service ping config-server
docker compose exec user-service ping kafka

# Container shell
docker compose exec user-service sh

# Environment variables in container
docker compose exec user-service env
```

---

**For general project information and quick start, see [README.md](README.md)**
**For end-to-end flow documentation, see [FLOW.md](FLOW.md)**
