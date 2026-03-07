# Smart Healthcare System - Infrastructure Documentation

Complete guide for infrastructure setup, deployment, and configuration.

## Table of Contents
- [Infrastructure Components](#infrastructure-components)
- [Service Port Mapping](#service-port-mapping)
- [Docker Setup](#docker-setup)
- [Database Configuration](#database-configuration)
- [Build & Deployment](#build--deployment)
- [Environment Variables](#environment-variables)
- [Networking](#networking)
- [Troubleshooting](#troubleshooting)

---

## Infrastructure Components

### Service Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client / Browser                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              API Gateway (8080)                          │
│              - JWT Validation                            │
│              - Request Routing                           │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────┐          ┌──────────────┐
│ Eureka Server│          │ Config Server│
│    (8761)    │          │    (8888)    │
└──────────────┘          └──────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│              Business Services                           │
│  User(8010) Admin(8011) Doctor(8012) Patient(8013)     │
│  Appointment(8014) Notification(8015)                   │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────┐          ┌──────────────┐
│ MySQL        │          │ MongoDB      │
│ (localhost)  │          │ (localhost)  │
└──────────────┘          └──────────────┘
```

### Infrastructure Services

#### Config Server (Port 8888)
**Purpose:** Centralized configuration management

**Features:**
- Stores configuration for all services
- Native profile (classpath-based)
- Configuration files in `src/main/resources/Configurations/`
- No Eureka registration (runs independently)

**Health Check:** http://localhost:8888/actuator/health

#### Eureka Server (Port 8761)
**Purpose:** Service discovery and registration

**Features:**
- Service registry for all microservices
- Health monitoring
- Load balancing support
- Dashboard UI

**Dashboard:** http://localhost:8761

#### API Gateway (Port 8080)
**Purpose:** Single entry point for all client requests

**Features:**
- JWT token validation
- Request routing to services
- Security filtering
- Header propagation (X-User-Email, X-User-Role)

**Routes:**
```
/api/user-service/**       → user-service
/api/admin-service/**      → admin-service
/api/doctor-service/**     → doctor-service
/api/patient-service/**    → patient-service
/api/appointment-service/** → appointment-service
```

**Security:**
- `/open/*` - Public endpoints (no authentication)
- `/secure/*` - Protected endpoints (JWT required)

---

## Service Port Mapping

### Complete Port Reference

| Service | Container | Internal | External | Network Name |
|---------|-----------|----------|----------|--------------|
| **Infrastructure** |
| Config Server | config-server | 8888 | 8888 | config-server |
| Eureka Server | eureka-server | 8761 | 8761 | eureka-server |
| API Gateway | gateway | 8080 | 8080 | gateway |
| **Business Services** |
| User Service | user-service | 8010 | 8010 | user-service |
| Admin Service | admin-service | 8011 | 8011 | admin-service |
| Doctor Service | doctor-service | 8012 | 8012 | doctor-service |
| Patient Service | patient-service | 8013 | 8013 | patient-service |
| Appointment Service | appointment-service | 8014 | 8014 | appointment-service |
| Notification Service | notification-service | 8015 | 8015 | notification-service |
| **Messaging** |
| Zookeeper | zookeeper | 2181 | 2181 | zookeeper |
| Kafka | kafka | 9092 | 9092 | kafka |
| Kafka UI | kafka-ui | 8080 | 9090 | kafka-ui |
| **Databases (Host)** |
| MySQL | - | 3306 | 3306 | localhost |
| MongoDB | - | 27017 | 27017 | localhost |

### Access URLs

**From Browser (localhost):**
```
Config Server:        http://localhost:8888/actuator/health
Eureka Dashboard:     http://localhost:8761
API Gateway:          http://localhost:8080
User Service:         http://localhost:8010/actuator/health
Admin Service:        http://localhost:8011/actuator/health
Doctor Service:       http://localhost:8012/actuator/health
Patient Service:      http://localhost:8013/actuator/health
Appointment Service:  http://localhost:8014/actuator/health
Notification Service: http://localhost:8015/actuator/health
Kafka UI:             http://localhost:9090
```

**From Docker Network:**
```
config-server:8888
eureka-server:8761
gateway:8080
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
- MySQL running on localhost:3306
- MongoDB running on localhost:27017
- All services built (JAR files in target/ folders)

### Build All Services
```bash
build-infrastructure.bat
```

**Build Time:** ~5-10 minutes for all services

### Docker Compose Configuration

**Network:**
- Name: `healthcare-network`
- Type: Bridge
- All containers communicate via this network

**Startup Order:**
```
1. Config Server (independent)
   ↓ (waits for health check)
2. Eureka Server
   ↓ (waits for health check)
3. Gateway + Zookeeper (parallel)
   ↓
4. Kafka + All Business Services (parallel)
   ↓
5. Kafka UI
```

### Docker Commands

**Start All Services:**
```bash
docker compose up -d
```

**Start Specific Services:**
```bash
# Infrastructure only
docker compose up -d config-server eureka-server gateway

# Add specific business service
docker compose up -d user-service doctor-service
```

**View Logs:**
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f user-service
```

**Check Status:**
```bash
docker compose ps
```

**Stop Services:**
```bash
# Stop all
docker compose down

# Stop specific service
docker compose stop user-service
```

**Rebuild After Code Changes:**
```bash
cd userService
mvnw clean package -DskipTests
cd ..
docker compose up -d --build user-service
```

---

## Database Configuration

### MySQL (User Service)

**Connection Details:**
- Host: localhost
- Port: 3306
- Database: userservice
- Username: From Vault.env (DATABASE_USERNAME)
- Password: From Vault.env (DATABASE_PASS)

**Setup:**
```sql
CREATE DATABASE userservice;
```

**From Docker Containers:**
- Host: `host.docker.internal`
- Port: 3306
- Configured via `MYSQL_HOST` environment variable

**Workbench Connection:**
```
Hostname: localhost
Port: 3306
Username: root
Password: root
```

### MongoDB (All Other Services)

**Connection Details:**
- Host: localhost
- Port: 27017
- Database: smart-healthcare-service
- No authentication (development)

**Collections:**
- Doctors
- Patients
- Appointments
- RequestRoles (Admin Service)

**From Docker Containers:**
- Host: `host.docker.internal`
- Port: 27017
- Configured via `MONGO_HOST` environment variable

**Compass Connection:**
```
Connection String: mongodb://localhost:27017
```

---

## Build & Deployment

### Build Process

**Automated Build:**
```bash
build-infrastructure.bat
```

**Manual Build:**
```bash
cd <serviceName>
mvnw clean package -DskipTests
cd ..
```

### Deployment Steps

**Step 1: Prerequisites Check**
```bash
# Check Java version
java -version  # Should be 17

# Check Docker
docker --version

# Check databases running on localhost:3306 and localhost:27017
```

**Step 2: Build Services**
```bash
build-infrastructure.bat
```

**Step 3: Start Infrastructure**
```bash
docker compose up -d config-server eureka-server gateway
```

**Step 4: Verify Infrastructure**
- Wait 30 seconds
- Check Eureka: http://localhost:8761

**Step 5: Start Business Services**
```bash
docker compose up -d
```

**Step 6: Verify All Services**
- Wait 1-2 minutes for registration
- Check Eureka dashboard - all services should appear

---

## Environment Variables

### Vault.env File

**Location:** Root directory

**Contents:**
```env
DATABASE_USERNAME=root
DATABASE_PASS=root
SECRET_KEY=asdfghjkqwertyuiomnbfre3456789{}{}
EXPIRATION=43200000
```

### Variable Usage

#### SECRET_KEY
**Used By:** Gateway, User Service
**Purpose:** JWT token signing and validation

#### EXPIRATION
**Used By:** User Service
**Purpose:** JWT token expiration time (milliseconds)

#### DATABASE_USERNAME
**Used By:** User Service
**Purpose:** MySQL connection username

#### DATABASE_PASS
**Used By:** User Service
**Purpose:** MySQL connection password

### Docker Environment Variables

#### CONFIG_HOST
**Set For:** Eureka, Gateway, All Business Services
**Value:** `config-server` (Docker network name)
**Default:** localhost

#### EUREKA_HOST
**Set For:** Gateway, All Business Services
**Value:** `eureka-server` (Docker network name)
**Default:** localhost

#### MYSQL_HOST
**Set For:** User Service
**Value:** `host.docker.internal`
**Default:** localhost

#### MONGO_HOST
**Set For:** Admin, Doctor, Patient, Appointment Services
**Value:** `host.docker.internal`
**Default:** localhost

---

## Networking

### Docker Network: healthcare-network

**Type:** Bridge
**Features:**
- Container-to-container communication
- DNS resolution by container name
- Access to host via `host.docker.internal`

### Service Discovery

**Within Docker Network:**
- Services use container names as hostnames
- Example: `http://user-service:8010`
- Eureka provides service registry

**From Host Machine:**
- Services accessible via localhost
- Example: `http://localhost:8010`

### Host Access from Containers

**Configuration:**
```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

**Usage:**
- MySQL: `host.docker.internal:3306`
- MongoDB: `host.docker.internal:27017`

---

## Troubleshooting

### Common Issues

#### 1. Services Not Starting

**Solutions:**
```bash
# Check logs
docker compose logs <service-name>

# Check if JAR exists
ls <serviceName>/target/*.jar

# Rebuild service
cd <serviceName>
mvnw clean package -DskipTests
```

#### 2. Service Not Registering with Eureka

**Solutions:**
- Wait 1-2 minutes (registration takes time)
- Check service logs: `docker compose logs -f <service-name>`
- Restart service: `docker compose restart <service-name>`

#### 3. Database Connection Failed

**Solutions:**
```bash
# Check if databases are running
# MySQL: localhost:3306
# MongoDB: localhost:27017

# Verify host.docker.internal
docker compose exec user-service ping host.docker.internal

# Check Vault.env credentials
```

#### 4. Port Already in Use

**Solutions:**
```bash
# Find process using port (Windows)
netstat -ano | findstr :8080

# Kill process
taskkill /PID <process-id> /F
```

#### 5. Config Server Not Found

**Solutions:**
- Ensure Config Server is healthy: http://localhost:8888/actuator/health
- Check Config Server logs: `docker compose logs config-server`
- Restart dependent services after Config Server is healthy

### Diagnostic Commands

```bash
# Check all container status
docker compose ps

# View resource usage
docker stats

# Check network connectivity
docker compose exec user-service ping config-server

# Access container shell
docker compose exec user-service sh

# View environment variables
docker compose exec user-service env
```

---

**For general project information and quick start guide, see [README.md](README.md)**
