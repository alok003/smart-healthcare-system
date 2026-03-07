# Smart Healthcare System

A comprehensive microservices-based healthcare management platform designed to streamline medical operations, patient care coordination, and administrative workflows.

## 🏥 Overview

The Smart Healthcare System provides a scalable, cloud-ready architecture for managing appointments, patient records, doctor profiles, and user authentication. Built using Spring Boot microservices architecture with service discovery, centralized configuration, and event-driven communication.

## ✨ Key Features

### Core Functionality
- **User Management** - Secure authentication and authorization with JWT tokens
- **Patient Management** - Patient profiles, medical history, and health vitals tracking
- **Doctor Management** - Doctor profiles, specializations, and availability management
- **Appointment System** - Scheduling, booking, and appointment lifecycle management
- **Admin Operations** - System administration and user management
- **Notifications** - Event-driven email notifications via Kafka

### Technical Features
- Microservices architecture with independent scaling
- Service discovery and registration (Eureka)
- Centralized configuration management (Spring Cloud Config)
- API Gateway with JWT-based security
- Event-driven messaging (Kafka)
- RESTful API design
- Docker containerization
- Health monitoring and actuator endpoints

## 🏗️ Architecture

### Microservices (9 Services)

**Infrastructure Services:**
- **Config Server** (8888) - Centralized configuration management
- **Eureka Server** (8761) - Service discovery and registration
- **API Gateway** (8080) - Single entry point with authentication

**Business Services:**
- **User Service** (8010) - Authentication and user management
- **Admin Service** (8011) - Administrative operations
- **Doctor Service** (8012) - Doctor profile management
- **Patient Service** (8013) - Patient records and health data
- **Appointment Service** (8014) - Appointment scheduling
- **Notification Service** (8015) - Email notifications

**Messaging Infrastructure:**
- **Kafka** (9092) - Message broker
- **Zookeeper** (2181) - Kafka coordination
- **Kafka UI** (9090) - Kafka monitoring interface

### Database Architecture
- **MySQL** - User Service (authentication and user data)
- **MongoDB** - Doctor, Patient, Appointment, Admin services (flexible healthcare data)

### Communication Patterns
- **Synchronous** - REST APIs via OpenFeign
- **Asynchronous** - Kafka events for notifications
- **Service Discovery** - Eureka-based dynamic service location

## 🛠️ Technology Stack

### Backend
- **Java 17** - Programming language
- **Spring Boot** 3.5.5 / 4.0.1 - Application framework
- **Spring Cloud** 2025.0.0 - Microservices ecosystem
  - Spring Cloud Config
  - Spring Cloud Netflix Eureka
  - Spring Cloud Gateway
  - Spring Cloud OpenFeign
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - MySQL database access
- **Spring Data MongoDB** - MongoDB database access

### Security
- **JWT** (JSON Web Tokens) - Token-based authentication
- **Spring Security** - Security framework
- **Role-based Access Control** - ADMIN, DOCTOR, PATIENT roles

### Databases
- **MySQL 8.0** - Relational database
- **MongoDB 7.0** - NoSQL document database

### Messaging
- **Apache Kafka 7.5.0** - Event streaming
- **Apache Zookeeper 7.5.0** - Kafka coordination

### Build & Deployment
- **Maven** - Build automation
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **Lombok** - Code generation

## 📋 Prerequisites

### Required Software
- **Java Development Kit (JDK) 17**
- **Maven 3.6+** (or use included wrapper)
- **Docker Desktop** with Docker Compose
- **MySQL 8.0** (running on localhost:3306)
- **MongoDB 7.0** (running on localhost:27017)
- **Git**

### Database Setup
1. **MySQL** - Create database `userservice`
   ```sql
   CREATE DATABASE userservice;
   ```

2. **MongoDB** - Database `smart-healthcare-service` will be created automatically

### Environment Variables
Ensure `Vault.env` file exists in the root directory:
```env
DATABASE_USERNAME=root
DATABASE_PASS=root
SECRET_KEY=your-secret-key-here
EXPIRATION=43200000
```

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
This builds all 9 microservices (takes 5-10 minutes).

### 3. Start Services
```bash
docker compose up -d
```

### 4. Verify Services
- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080/actuator/health
- **Config Server**: http://localhost:8888/actuator/health
- **Kafka UI**: http://localhost:9090

All services should register with Eureka within 1-2 minutes.

## 📡 API Access

### Gateway Routes
All API requests go through the Gateway (http://localhost:8080):

```
User Service:        http://localhost:8080/api/user-service/**
Admin Service:       http://localhost:8080/api/admin-service/**
Doctor Service:      http://localhost:8080/api/doctor-service/**
Patient Service:     http://localhost:8080/api/patient-service/**
Appointment Service: http://localhost:8080/api/appointment-service/**
```

### Authentication
1. **Register User**: `POST /api/user-service/open/newUser`
2. **Login**: `POST /api/user-service/open/login`
3. **Use JWT Token**: Add `Authorization: Bearer <token>` header for secured endpoints

### Public vs Secured Endpoints
- **Public** (`/open/*`) - No authentication required
- **Secured** (`/secure/*`) - JWT token required

## 🔧 How It Works

### Service Startup Flow
```
1. Config Server starts (provides configuration)
2. Eureka Server starts (service registry)
3. Gateway starts (API routing)
4. Business services start (register with Eureka)
5. Services discover each other via Eureka
```

### Request Flow
```
Client → Gateway (JWT validation) → Service Discovery (Eureka) 
→ Target Service → Database → Response
```

### Authentication Flow
```
1. User submits credentials to /api/user-service/open/login
2. User Service validates against MySQL
3. JWT token generated with email and role
4. Client includes token in subsequent requests
5. Gateway validates token and extracts user info
6. Gateway adds X-User-Email and X-User-Role headers
7. Target service processes request
```

### Inter-Service Communication
- Services use OpenFeign clients for REST calls
- Service names resolved via Eureka (e.g., `http://doctor-service/api/...`)
- Load balancing handled automatically

### Event-Driven Notifications
- Services publish events to Kafka topics
- Notification Service consumes events
- Emails sent asynchronously

## 🗂️ Project Structure

```
smart-healthcare-system/
├── configServer/          # Configuration management
├── eurekaServer/          # Service discovery
├── gateway/               # API Gateway
├── userService/           # User authentication
├── adminService/          # Admin operations
├── doctorService/         # Doctor management
├── patientService/        # Patient management
├── appointmentService/    # Appointment scheduling
├── notificationService/   # Email notifications
├── docker-compose.yml     # Docker orchestration
├── build-infrastructure.bat  # Build script
├── Vault.env             # Environment variables
└── README.md             # This file
```

Each service follows standard Spring Boot structure:
```
<serviceName>/
├── src/main/java/com/project/<serviceName>/
│   ├── Controller/       # REST endpoints
│   ├── Entity/          # Database entities
│   ├── Model/           # DTOs and domain models
│   ├── Repository/      # Data access layer
│   ├── Service/         # Business logic
│   ├── Exception/       # Custom exceptions
│   ├── Utility/         # Helper classes
│   └── Configuration/   # Spring configuration
├── src/main/resources/
│   └── application.yml  # Service configuration
├── Dockerfile           # Container definition
└── pom.xml             # Maven dependencies
```

## 🔍 Service Details

### User Service (Port 8010)
- User registration and authentication
- JWT token generation
- Password encryption
- Role management (ADMIN, DOCTOR, PATIENT)
- MySQL database

### Doctor Service (Port 8012)
- Doctor profile management
- Specialization tracking
- Availability management
- License verification
- MongoDB database

### Patient Service (Port 8013)
- Patient profile management
- Medical history tracking
- Health vitals monitoring
- Appointment history
- MongoDB database

### Appointment Service (Port 8014)
- Appointment scheduling
- Status management (UPCOMING, VISITED, CANCELLED)
- Health check records
- Prescription management
- MongoDB database

### Admin Service (Port 8011)
- User role management
- Doctor approval workflow
- System administration
- MongoDB database

### Notification Service (Port 8015)
- Kafka event consumption
- Email notifications
- Thymeleaf templates
- SMTP integration

## 🧪 Development

### Running Locally (Without Docker)
1. Start Config Server: `cd configServer && mvnw spring-boot:run`
2. Start Eureka Server: `cd eurekaServer && mvnw spring-boot:run`
3. Start Gateway: `cd gateway && mvnw spring-boot:run`
4. Start business services similarly

### Building Individual Service
```bash
cd <serviceName>
mvnw clean package -DskipTests
```

### Viewing Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f user-service
```

### Stopping Services
```bash
# Stop all
docker compose down

# Stop specific service
docker compose stop user-service
```

### Rebuilding After Changes
```bash
cd <serviceName>
mvnw clean package -DskipTests
docker compose up -d --build <service-name>
```

## 📊 Monitoring

### Eureka Dashboard
- URL: http://localhost:8761
- View all registered services
- Check service health status

### Actuator Endpoints
Each service exposes health endpoints:
```
http://localhost:<port>/actuator/health
http://localhost:<port>/actuator/info
```

### Kafka UI
- URL: http://localhost:9090
- Monitor topics and messages
- View consumer groups

## 🔐 Security

### JWT Configuration
- Secret key stored in `Vault.env`
- Token expiration: 12 hours (configurable)
- Claims: email, role

### Role-Based Access
- **USER** - Basic user access
- **PATIENT** - Patient-specific operations
- **DOCTOR** - Doctor-specific operations
- **ADMIN** - Administrative operations

### Database Security
- Credentials stored in `Vault.env`
- Password encryption using BCrypt
- Parameterized queries (SQL injection prevention)

## 🐛 Troubleshooting

### Services Not Starting
- Check if MySQL and MongoDB are running
- Verify `Vault.env` file exists
- Check Docker Desktop is running
- View logs: `docker compose logs -f`

### Service Not Registering with Eureka
- Wait 1-2 minutes for registration
- Check Eureka dashboard: http://localhost:8761
- Verify network connectivity: `docker network inspect healthcare-network`

### Database Connection Issues
- Ensure MySQL is on localhost:3306
- Ensure MongoDB is on localhost:27017
- Check credentials in `Vault.env`
- Test connection with Workbench/Compass

### Port Conflicts
- Check if ports are already in use
- Stop conflicting services
- Modify port mappings in docker-compose.yml if needed

## 📚 Additional Documentation

**For detailed infrastructure setup and deployment instructions, see [INFRASTRUCTURE.md](INFRASTRUCTURE.md)**

## 🤝 Contributing

1. Follow existing code structure and naming conventions
2. Use Lombok annotations (@Data, @AllArgsConstructor)
3. Add validation annotations on models
4. Include health endpoints in controllers
5. Update configuration files for new services
6. Test locally before committing

## 📄 License

[Add your license information here]

## 👥 Team

[Add team member information here]

---

**For detailed infrastructure setup and deployment instructions, see [INFRASTRUCTURE.md](INFRASTRUCTURE.md)**
