# 🚀 Uptime Sentinel

A cloud-ready **Uptime Monitoring and Incident Reporting Platform** built with **Spring Boot Microservices**, designed to monitor websites and APIs, detect downtime, and allow users to report incidents with screenshot evidence.

The platform combines a **Spring Cloud microservices architecture** with **AWS S3 and AWS Lambda** for serverless evidence processing.

---

## 📌 Overview

Uptime Sentinel provides centralized monitoring, authentication, service discovery, configuration management, and incident reporting.

The system allows users to:

- Monitor websites and APIs
- Track uptime and response times
- Detect service failures
- Report incidents and downtime
- Attach screenshot/image evidence to incidents
- Store evidence securely in Amazon S3
- Process incident evidence using AWS Lambda
- Track Lambda execution status
- Manage reported issues through REST APIs

---

# 🏗️ Architecture

```text
                         ┌─────────────────────┐
                         │    Client / React   │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    API Gateway      │
                         │ Spring Cloud        │
                         │ Gateway              │
                         └──────────┬──────────┘
                                    │
                       JWT / OAuth2 Authentication
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
             ▼                      ▼                      ▼
     ┌──────────────┐      ┌────────────────┐      Future Services
     │ Eureka       │      │ Monitoring     │
     │ Registry     │      │ Service        │
     └──────────────┘      └───────┬────────┘
                                    │
                          ┌─────────┴──────────┐
                          │                    │
                          ▼                    ▼
                   ┌─────────────┐      ┌─────────────┐
                   │ PostgreSQL  │      │   AWS S3    │
                   │             │      │ Issue Proof │
                   └─────────────┘      │   Images    │
                                        └──────┬──────┘
                                               │
                                        ObjectCreated /
                                        Lambda Invocation
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ AWS Lambda  │
                                        │ Node.js 20  │
                                        └─────────────┘

              ┌─────────────────────┐
              │ Spring Cloud Config │
              │       Server        │
              └─────────────────────┘

              ┌─────────────────────┐
              │    Cloud / AWS      │
              │ Serverless Services │
              └─────────────────────┘
```

---

# ✨ Core Features

## 🔐 Authentication & Security

- OAuth2 authentication
- JWT-based authorization
- Role-based access control
- Stateless authentication
- Secure REST APIs
- Password encryption
- CORS configuration

---

## 🌐 API Gateway

Spring Cloud Gateway acts as the centralized entry point.

Responsibilities include:

- Request routing
- JWT validation
- Authentication filtering
- Service discovery integration
- Global exception handling
- CORS configuration
- Load balancing

---

## 🔎 Service Discovery

The project uses **Netflix Eureka** for service discovery.

Features:

- Automatic service registration
- Dynamic service discovery
- Health monitoring
- Service-to-service communication
- Reduced dependency on hardcoded service URLs

---

## ⚙️ Centralized Configuration

The **Spring Cloud Config Server** provides centralized configuration management.

Features:

- Externalized configuration
- Environment-specific configuration
- Centralized properties
- Remote configuration management

---

# 📊 Monitoring Service

The Monitoring Service contains the core uptime-monitoring business logic.

### Monitoring capabilities

- Website uptime monitoring
- API health checks
- Scheduled monitoring
- Failure detection
- Response-time tracking
- Availability tracking
- Monitor status management
- Monitoring reports

The service periodically checks configured endpoints and records their health and response information.

---

# 🚨 Issue & Incident Reporting

Uptime Sentinel also includes an incident reporting workflow that allows users to report problems detected during monitoring.

Users can create issue tickets containing:

- Issue title
- Description
- Category
- Priority
- Related website/monitor
- Screenshot/image proof
- User information
- Issue status
- AWS S3 storage information
- AWS Lambda execution information
- Creation and update timestamps

---

# ☁️ AWS S3 Integration

Incident screenshot evidence is stored in **Amazon S3**.

### Supported image formats

```text
PNG
JPEG
WEBP
GIF
```

### Maximum file size

```text
10 MB
```

Uploaded files use partitioned S3 keys:

```text
issues/{userEmail}/{uuid}_{filename}
```

Example:

```text
issues/user@example.com/
a7c9d123_screenshot.png
```

This prevents filename collisions and keeps uploaded evidence organized by user.

---

# ⚡ AWS Lambda Integration

The monitoring backend integrates with an AWS Lambda function for serverless issue-proof processing.

When an issue is created, the backend sends an event containing information such as:

```json
{
  "issueId": 123,
  "s3Bucket": "uptime-sentinel",
  "s3Key": "issues/user@example.com/abc123_screenshot.png",
  "screenshotUrl": "...",
  "category": "DOWNTIME",
  "priority": "HIGH",
  "userEmail": "user@example.com"
}
```

The Lambda function processes the issue proof and performs validation/priority-related processing.

The backend records:

- Lambda execution status
- Lambda function ARN
- Processing result
- Associated issue information

---

# 🛠️ AWS Integration Components

### `AwsConfig.java`

Provides AWS client configuration for:

- Amazon S3
- AWS Lambda

The configuration also performs credential validation and supports local development environments.

### `AwsS3Service.java`

Responsible for:

- Image validation
- File-size validation
- S3 uploads
- S3 object deletion
- S3 key generation
- Local development fallback

### `AwsLambdaService.java`

Responsible for:

- Building Lambda event payloads
- Invoking Lambda
- Tracking execution status
- Recording Lambda ARN
- Local development fallback

---

# ⚙️ Local Development Fallback

The AWS integration supports development environments where AWS credentials are unavailable.

When AWS credentials are not configured, the backend can use a local simulation/fallback mechanism instead of failing immediately.

This allows developers to work on the monitoring and issue-reporting functionality without requiring AWS access for every local run.

> AWS credentials and production configuration should be used when deploying the application to AWS.

---

# 🧩 Issue Reporting APIs

## Create Issue

```http
POST /api/monitoring/issues
```

Multipart form data:

```text
title
description
category
priority
websiteId
proof
```

The endpoint:

1. Validates the request
2. Validates the proof image
3. Uploads the image to S3
4. Creates the issue record
5. Invokes AWS Lambda
6. Stores Lambda execution information

---

## Get User Issues

```http
GET /api/monitoring/issues
```

Returns issues associated with the authenticated user.

---

## Get Issue Details

```http
GET /api/monitoring/issues/{id}
```

Returns detailed information about a specific issue.

---

## Update Issue Status

```http
PATCH /api/monitoring/issues/{id}/status
```

Used to update the issue lifecycle/status.

---

## Delete Issue

```http
DELETE /api/monitoring/issues/{id}
```

Deletes the issue and
