# AttenTrack

AttenTrack is a production-grade attendance platform built around a Spring Boot application and a dedicated OCR microservice. It supports local development via Docker Compose and cloud deployment on AWS EKS with Terraform-managed infrastructure and GitHub Actions delivery.

## What This Project Includes

- Student and admin attendance workflows.
- Attendance summary and eligibility analytics.
- OCR course extraction microservice (Python + PaddleOCR).
- Redis-backed caching.
- PostgreSQL on RDS for production data.
- Kubernetes manifests for app, OCR, Redis, and monitoring.
- Terraform infrastructure for VPC, EKS, RDS, and ECR.
- CI/CD pipeline that builds images and deploys to EKS from shared Terraform state.

## Service Architecture

Production deployment uses three application services:

- AttenTrack app service (Java/Spring Boot): API + web UI.
- OCR service (Python/Flask): image-to-course parsing API.
- Redis service: cache and health dependency for app runtime.

Main runtime dependencies:

- App -> RDS PostgreSQL
- App -> Redis
- App -> OCR service

## Tech Stack

### Application

- Java 21
- Spring Boot 4.x
- Spring Security + JWT
- Spring Data JPA (Hibernate)
- Thymeleaf
- Redis

### OCR Microservice

- Python 3
- Flask
- PaddleOCR + PaddlePaddle
- Pillow + NumPy
- Gunicorn

### Infrastructure and Delivery

- Docker and Docker Compose
- Kubernetes (EKS)
- Terraform
- GitHub Actions
- AWS ECR, EKS, RDS, S3, DynamoDB, Secrets Manager

### Observability

- Spring Actuator + Micrometer
- Prometheus
- Grafana

## Repository Layout

```text
.
|- src/                               # Spring Boot application
|- services/ocr-service/              # OCR microservice (Flask)
|- k8s/                               # Kubernetes manifests
|  |- deployment_and_service.yaml     # App service + deployment
|  |- ocr-deployment-and-service.yaml # OCR service + deployment
|  |- redis-deployment-and-service.yaml
|  |- monitoring.yaml
|- terraform/                         # Infrastructure as code
|- .github/workflows/ecr-push.yml     # CI/CD workflow
|- docker-compose.yml                 # Local multi-service stack
|- prod.md                            # Production runbook
```

## Local Development

### Prerequisites

- Java 21
- Maven (or mvnw)
- Docker
- Docker Compose

### Quick Start (Recommended)

```bash
docker compose up --build
```

This starts:

- app on port 8080 -> container 8081
- ocr-service on port 5000
- redis on port 6379

Optional monitoring profile:

```bash
docker compose --profile monitoring up --build
```

### Run Spring App Without Compose

```bash
./mvnw spring-boot:run
```

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

## Configuration

Key runtime variables:

- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- SPRING_DATA_REDIS_HOST, SPRING_DATA_REDIS_PORT
- OCR_API_BASE_URL, OCR_API_KEY
- JWT_SECRET

Production profile is enabled via:

- SPRING_PROFILES_ACTIVE=prod

## OCR Microservice Details

The OCR microservice provides course extraction from uploaded images and includes:

- Lazy OCR engine initialization with retry behavior.
- Health endpoint for liveness.
- Ready endpoint for readiness.
- Request auth via OCR API key.
- In-memory result cache for repeated OCR content.

Kubernetes manifest:

- k8s/ocr-deployment-and-service.yaml

## Redis in Production

Redis is deployed inside the cluster and app pods are explicitly configured to use it.

Kubernetes manifest:

- k8s/redis-deployment-and-service.yaml

App deployment env:

- SPRING_DATA_REDIS_HOST=redis
- SPRING_DATA_REDIS_PORT=6379

This avoids localhost fallback and prevents readiness instability.

## API Overview

Authentication:

- POST /api/auth/login
- POST /api/auth/register
- POST /api/auth/register-admin
- POST /api/auth/logout

Student routes:

- /api/student/**

Admin routes:

- /api/admin/**

## Kubernetes Deployment

For production, CI/CD handles deployment automatically. For manual apply, use:

```bash
kubectl apply -f k8s/redis-deployment-and-service.yaml
kubectl apply -f k8s/ocr-deployment-and-service.yaml
kubectl apply -f k8s/deployment_and_service.yaml
```

Rollout checks:

```bash
kubectl rollout status deployment/redis-deployment -n default --timeout=5m
kubectl rollout status deployment/ocr-deployment -n default --timeout=15m
kubectl rollout status deployment/attentrack-deployment -n default --timeout=15m
```

## Terraform and Shared State

This project uses shared remote Terraform state so local infra operations and CI/CD read the same outputs.

Backend components:

- S3 bucket for tfstate
- DynamoDB table for state locking

Complete one-time setup and migration steps are documented in prod.md.

## CI/CD

Workflow file:

- .github/workflows/ecr-push.yml

Trigger:

- push to main
- manual workflow dispatch

Pipeline flow:

1. Preflight backend and Terraform output checks.
2. Build and push app + OCR images to ECR.
3. Resolve DB credentials from AWS Secrets Manager.
4. Apply Redis, OCR, and app manifests.
5. Verify rollout order: Redis -> OCR -> app.

Required GitHub secrets:

- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- OCR_API_KEY

No GitHub DB secrets are required.

## Production Runbook

Use this for full production onboarding and repeatable operations:

- [prod.md](prod.md)

It covers:

- one-time S3 + DynamoDB backend setup
- one-time state migration
- terraform apply flow
- IAM requirements
- deployment verification steps

## Contribution

- Create focused branches and small commits.
- Run tests before PR:

```bash
./mvnw test
```

- Include rollout notes for k8s or infra changes.

## License

Use and distribution follow the repository license policy.