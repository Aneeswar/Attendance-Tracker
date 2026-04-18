# AttenTrack - Student Attendance Tracking System

AttenTrack is a production-oriented, microservice-ready attendance management platform for educational institutions. It combines robust attendance business logic with cloud-native deployment on AWS EKS, infrastructure automation via Terraform, and observability through Prometheus and Grafana.

## Project Overvieww

- Tracks and analyzes student attendance across the semester lifecycle.
- Enforces role-based access for Admin and Student workflows.
- Applies academic calendar, holiday, and exam-window rules to attendance projections.
- Supports containerized deployments with scalable orchestration on Kubernetes.
- Demonstrates end-to-end backend + DevOps ownership: application, infrastructure, CI/CD, and monitoring.

## Key Features

- Role-based access control (RBAC): `ADMIN` and `STUDENT` capabilities are strictly separated.
- Attendance modes:
  - Aggregate attendance updates (conducted vs attended)
  - Date-based attendance marking with calendar-aware validation
- Business-rule-driven attendance engine:
  - Working-day validation (semester boundaries, weekends, holiday exclusions)
  - Exam-window aware calculations (`CAT-1`, `CAT-2`, `FAT`)
  - Status classification: `SAFE`, `AT_RISK`, `IMPOSSIBLE`
- Semester-aware holiday handling with scope support:
  - `FULL`, `MORNING`, `AFTERNOON` half-day logic
  - Holiday requests and admin approval workflow
- Student and admin analytics/reporting endpoints.
- Cloud deployment readiness:
  - Dockerized runtime
  - Kubernetes manifests for app and monitoring
  - AWS EKS + RDS architecture provisioned with Terraform

## Engineering Impact

- Scalability: stateless Spring Boot service deployed as multi-replica Kubernetes workloads behind AWS load balancers.
- Reliability: Multi-AZ PostgreSQL RDS, health probes, and rollout-aware deployments.
- Automation: GitHub Actions pipeline builds, pushes, and deploys immutable container images.
- Infrastructure as Code: reproducible AWS environment (VPC, EKS, RDS, ECR) using Terraform modules.
- Operability: metrics-driven monitoring stack with Prometheus scraping and Grafana dashboards.

## Tech Stack

### Backend

- Java 21
- Spring Boot 4.x
- Spring Security (JWT + method-level authorization)
- Spring Data JPA (Hibernate)
- Thymeleaf (server-rendered UI)
- Redis (cache layer)

### Cloud (AWS)

- Amazon EKS (Kubernetes control plane + managed node groups)
- Amazon RDS for PostgreSQL (private subnets, Multi-AZ)
- Amazon ECR (container image registry)
- Amazon VPC (public/private/intra subnet layout with NAT)
- IAM (CI/CD and cluster access integration)
- CloudWatch (AWS-native operational telemetry in deployed environments)

### DevOps

- Docker (multi-stage image build)
- Kubernetes manifests (`k8s/`)
- Terraform (modular IaC under `terraform/`)
- GitHub Actions CI/CD (`.github/workflows/ecr-push.yml`)

### Monitoring

- Spring Boot Actuator + Micrometer
- Prometheus
- Grafana

## System Architecture

- Users access AttenTrack through an AWS LoadBalancer-backed Kubernetes Service.
- EKS routes requests to Spring Boot pods (`attentrack-deployment`, configurable replicas).
- Application pods connect to PostgreSQL on Amazon RDS over private networking.
- RDS access is restricted via security groups to EKS worker-node traffic on port `5432`.
- CI/CD publishes images to ECR, then updates running EKS deployments with new image tags.
- Prometheus scrapes `/actuator/prometheus`; Grafana visualizes application and platform metrics.

## Folder Structure

```text
.
|- src/
|  |- main/
|  |  |- java/com/deepak/Attendance/
|  |  |  |- config/         # Spring + security configuration
|  |  |  |- controller/     # REST + web controllers
|  |  |  |- dto/            # Request/response DTOs
|  |  |  |- entity/         # JPA entities
|  |  |  |- repository/     # Data access interfaces
|  |  |  |- security/       # JWT filter/token utilities
|  |  |  |- service/        # Business logic and attendance engine
|  |  |- resources/         # application properties, templates, static assets
|  |- test/java/            # unit and controller tests
|- k8s/                     # Kubernetes manifests (app + monitoring)
|- terraform/               # AWS infrastructure code (VPC, EKS, RDS, ECR)
|- grafana/                 # provisioning assets
|- Dockerfile
|- docker-compose.yml
|- pom.xml
```

## Setup Instructions

### Prerequisites

- Java 21
- Maven 3.8+ (or project wrapper `mvnw` / `mvnw.cmd`)
- PostgreSQL 15+
- Docker + Docker Compose
- AWS CLI, `kubectl`, Terraform >= 1.5 (for cloud deployment)

### Local Setup (Spring Boot + PostgreSQL)

1. Create PostgreSQL database:

```sql
CREATE DATABASE attendance_db;
```

2. Configure application properties (or environment variables).
   - Uses `src/main/resources/application.properties`
   - Defaults support local development when vars are not set

3. Start the application:

```bash
./mvnw spring-boot:run
```

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Default application URL: `http://localhost:8081`

### Environment Variables

Application/runtime variables:

- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `5432`)
- `DB_NAME` (default: `attendance_db`)
- `DB_USERNAME` (default: `postgres`)
- `DB_PASSWORD`
- `JWT_SECRET`
- `PORT` (default: `8081`)
- `SPRING_DATA_REDIS_HOST` (default: `localhost`)
- `SPRING_DATA_REDIS_PORT` (default: `6379`)
- `SPRING_DATA_REDIS_PASSWORD` (optional)

Infrastructure (Terraform) variables:

- `region`
- `cluster_name`
- `environment`
- `db_name`
- `db_username`
- `db_password` (sensitive)
- `github_actions_iam_arn`

## Running the Project

### Local Run

```bash
./mvnw clean spring-boot:run
```

### Docker

Build image:

```bash
docker build -t attentrack:local .
```

Run with compose:

```bash
docker compose up --build
```

Optional monitoring profile:

```bash
docker compose --profile monitoring up --build
```

### Kubernetes (EKS)

1. Provision infrastructure:

```bash
cd terraform
terraform init
terraform apply
```

2. Configure kube context:

```bash
aws eks update-kubeconfig --region <aws-region> --name <cluster-name>
```

3. Apply Kubernetes resources:

```bash
kubectl apply -f k8s/db-secret.yaml
kubectl apply -f k8s/gf-config.yaml
kubectl apply -f k8s/monitoring.yaml
kubectl apply -f k8s/deployment_and_service.yaml
```

4. Verify rollout:

```bash
kubectl rollout status deployment/attentrack-deployment
```

## API Overview

Authentication:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/register-admin`
- `POST /api/auth/logout`

Student APIs:

- `/api/student/**` for timetable, attendance entry, attendance reports, course operations, profile updates, holiday requests
- `/api/student/attendance/**` for date-based attendance calendar/update flows
- `/api/student/academic-calendar` for student calendar access

Admin APIs:

- `/api/admin/students/**` for student administration and reporting
- `/api/admin/courses/**` for course catalog/assignment operations
- `/api/admin/holidays/**` for holiday CRUD/bulk/range operations
- `/api/admin/academic-calendar/**` for semester and exam-window management
- `/api/admin/holiday-requests/**` for approval/rejection workflows

## Security

- Stateless JWT authentication using Spring Security filters.
- Tokens supported via `Authorization: Bearer <token>` and JWT cookie extraction.
- Endpoint-level authorization with role mapping:
  - `ROLE_STUDENT` for student routes
  - `ROLE_ADMIN` for admin routes
- Method-level access control via `@PreAuthorize`.
- Password hashing with `BCryptPasswordEncoder`.
- CORS enabled for expected local origins; CSRF disabled for stateless API flow.

## DevOps and CI/CD Pipeline

GitHub Actions workflow: `.github/workflows/ecr-push.yml`

- Trigger: push to `main`
- Pipeline stages:
  - Checkout source
  - Configure AWS credentials
  - Authenticate to ECR
  - Build Docker image tagged with commit SHA
  - Push image to ECR repository `attentrack`
  - Connect to EKS cluster
  - Auto-discover RDS endpoint
  - Apply Kubernetes manifests
  - Inject runtime env vars (`DB_HOST`) into deployment
  - Update deployment image and wait for rollout completion

## Infrastructure as Code (Terraform)

Terraform modules/resources define production infrastructure for:

- VPC:
  - Public, private, and intra subnets across AZs
  - NAT gateway support for private workloads
- EKS:
  - Managed node groups
  - Cluster access entries for CI/CD principal
- RDS PostgreSQL:
  - Private subnet placement
  - Multi-AZ enabled
  - Security-group restricted ingress from EKS nodes
- ECR:
  - Repository with scan-on-push
  - Lifecycle policy to retain recent images

## Monitoring and Logging

- Metrics exposure through `/actuator/prometheus`.
- Prometheus deployment and scrape config in `k8s/monitoring.yaml`.
- Grafana deployment with preconfigured Prometheus datasource.
- Kubernetes pod annotations enable metrics scraping.
- Health probes:
  - `/actuator/health/liveness`
  - `/actuator/health/readiness`
- CloudWatch can be integrated in AWS environments for cluster/node/application log centralization.

## Deployment Architecture

- Containerized Spring Boot service runs on EKS with rolling updates.
- Service type `LoadBalancer` provides external traffic entry.
- App pods remain stateless; persistence is externalized to PostgreSQL RDS.
- DB credentials are injected from Kubernetes Secrets.
- Runtime config (including discovered RDS endpoint) is injected during deployment.
- Horizontal scaling is achieved by adjusting deployment replicas and node-group capacity.

## Future Improvements

- Add Horizontal Pod Autoscaler (HPA) based on CPU and custom metrics.
- Use AWS Secrets Manager / External Secrets Operator for secret management.
- Add distributed tracing (OpenTelemetry + AWS X-Ray/Jaeger).
- Introduce blue-green or canary deployment strategy.
- Expand notification workflows (email/SMS) for low attendance alerts.
- Expose versioned OpenAPI/Swagger documentation for API consumers.

## Contribution Guidelines

- Fork the repository and create a feature branch.
- Follow clean commit practices with focused changes.
- Maintain test coverage for service/controller updates.
- Validate locally before PR:

```bash
./mvnw test
```

- For infra updates, include Terraform plan output summary in PR notes.
- For deployment-manifest changes, include rollout/rollback validation steps.

## License

Use and distribution follow the repository's license policy.