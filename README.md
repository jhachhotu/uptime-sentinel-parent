# 🚀 Uptime Monitoring System

A production-ready **Uptime Monitoring Platform** built using **Spring Boot Microservices Architecture**, designed to monitor the health and availability of web applications, APIs, and services. The system provides centralized configuration, service discovery, API gateway routing, secure authentication, and real-time monitoring in a scalable cloud-native architecture.

---

## 📌 Overview

The project follows a **Microservices Architecture** where each service is independently deployable, scalable, and maintainable.

It leverages the Spring Cloud ecosystem to provide:

- Service Discovery
- Centralized Configuration
- API Gateway
- OAuth2 & JWT Authentication
- Health Monitoring
- Dockerized Deployment
- RESTful APIs
- Secure Communication

---

# 🏗️ Architecture

```text
                   +----------------------+
                   |      Client/App      |
                   +----------+-----------+
                              |
                              |
                    API Gateway (Spring Cloud Gateway)
                              |
        -------------------------------------------------
        |                     |                         |
        |                     |                         |
 Registry Service      Monitoring Service      Future Services
 (Eureka Server)         (Business Logic)
        |
 Config Server
        |
  Git Configuration Repository

```

---

# ✨ Features

## 🔐 Security

- OAuth2 Authentication
- JWT Token-based Authorization
- Role-Based Access Control (RBAC)
- Secure REST APIs
- Password Encryption

---

## 🌐 API Gateway

- Centralized API Routing
- Authentication Filter
- Request Validation
- Load Balancing
- Global Exception Handling

---

## ⚙️ Service Discovery

- Netflix Eureka Server
- Automatic Service Registration
- Dynamic Service Discovery
- Fault Tolerance

---

## 📦 Configuration Management

- Spring Cloud Config Server
- Externalized Configuration
- Environment-Based Configurations
- Centralized Property Management

---

## 📊 Monitoring Service

- Website Uptime Monitoring
- API Health Checks
- Status Tracking
- Scheduled Monitoring
- Failure Detection
- Response Time Monitoring
- Availability Reporting

---

## 🐳 Docker Support

- Dockerized Microservices
- Docker Compose
- Easy Deployment
- Multi-container Architecture

---

## ⚡ Scalable Architecture

- Independent Services
- Loose Coupling
- Easy Scaling
- Cloud Ready
- Production Ready

---

# 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security |
| Authentication | OAuth2 + JWT |
| API Gateway | Spring Cloud Gateway |
| Service Discovery | Netflix Eureka |
| Configuration | Spring Cloud Config |
| Monitoring | Spring Boot Actuator |
| Build Tool | Maven |
| Database | PostgreSQL / MySQL |
| Containerization | Docker |
| Orchestration | Docker Compose |

---

# 📂 Project Structure

```
uptime-monitoring/

│
├── api-gateway/
│
├── config-server/
│
├── registry-service/
│
├── monitoring-service/
│
├── common/
│
├── docker-compose.yml
│
├── Dockerfile
│
├── Dockerfile.gateway
│
├── Dockerfile.config
│
├── Dockerfile.monitoring
│
├── Dockerfile.registry
│
├── .env.example
│
└── README.md
```

---

# 🏢 Microservices

## 📌 API Gateway

Responsibilities

- Central Entry Point
- Request Routing
- JWT Validation
- Authentication
- Authorization
- Global Filters
- Rate Limiting (Future)

---

## 📌 Registry Service

Responsibilities

- Eureka Server
- Service Registration
- Service Discovery
- Health Monitoring

---

## 📌 Config Server

Responsibilities

- Centralized Configuration
- Environment Management
- Remote Configuration
- Configuration Refresh

---

## 📌 Monitoring Service

Responsibilities

- Uptime Checks
- Website Monitoring
- API Monitoring
- Response Time Analysis
- Service Health Tracking
- Alert Generation
- Monitoring Reports

---

## 📌 Common Module

Contains

- Shared DTOs
- Utility Classes
- Common Exceptions
- Constants
- Security Utilities
- Shared Configurations

---

# 🔐 Authentication Flow

```text
User

   │

   ▼

Login Request

   │

   ▼

OAuth2 Authentication

   │

   ▼

Generate JWT Token

   │

   ▼

Client Stores Token

   │

   ▼

Authorization Header

Bearer <JWT Token>

   │

   ▼

API Gateway validates Token

   │

   ▼

Forward Request to Microservice
```

---

# 🚀 Getting Started

## Clone Repository

```bash
git clone https://github.com/yourusername/uptime-monitoring-system.git

cd uptime-monitoring-system
```

---

## Build Project

```bash
mvn clean install
```

---

## Run Config Server

```bash
cd config-server

mvn spring-boot:run
```

---

## Run Eureka Server

```bash
cd registry-service

mvn spring-boot:run
```

---

## Run Monitoring Service

```bash
cd monitoring-service

mvn spring-boot:run
```

---

## Run API Gateway

```bash
cd api-gateway

mvn spring-boot:run
```

---

# 🐳 Docker Deployment

Build Containers

```bash
docker-compose build
```

Start Containers

```bash
docker-compose up -d
```

Stop Containers

```bash
docker-compose down
```

---

# 📖 API Documentation

Swagger documentation is available for each microservice.

```
http://localhost:<PORT>/swagger-ui/index.html
```

Example

```
http://localhost:8080/swagger-ui/index.html
```

---

# 📈 Monitoring

The application supports monitoring through Spring Boot Actuator.

Example Endpoints

```
/actuator/health

/actuator/info

/actuator/metrics

/actuator/prometheus
```

---

# 🔄 Request Flow

```text
Client

   │

API Gateway

   │

JWT Authentication

   │

Service Discovery (Eureka)

   │

Monitoring Service

   │

Database

   │

Response

   │

Client
```

---

# 🔒 Security Features

- OAuth2 Authentication
- JWT Authorization
- Spring Security
- Secure REST APIs
- Password Encryption
- Token Validation
- Stateless Authentication
- CORS Configuration

---

# 🌟 Future Enhancements

- Email Notifications
- SMS Alerts
- Slack Integration
- Discord Notifications
- Prometheus Integration
- Grafana Dashboard
- Kubernetes Deployment
- RabbitMQ/Kafka Event Streaming
- Distributed Tracing
- OpenTelemetry
- Circuit Breaker (Resilience4j)
- Rate Limiting
- Audit Logging
- Multi-Tenant Support

---

# 🤝 Contributing

Contributions are welcome!

1. Fork the repository

2. Create a feature branch

```bash
git checkout -b feature/new-feature
```

3. Commit your changes

```bash
git commit -m "Add new feature"
```

4. Push your branch

```bash
git push origin feature/new-feature
```

5. Create a Pull Request

---

# 📄 License

This project is licensed under the **MIT License**.

---

# 👨‍💻 Author

**Your Name**

- GitHub: https://github.com/jhachhotu
- LinkedIn: https://www.linkedin.com/in/kumarchhotu/

---

## ⭐ If you found this project useful, don't forget to star the repository!
