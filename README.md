# AttenTrack - Attendance Tracker & Management System

AttenTrack is a sophisticated Spring Boot-based attendance tracking and management system designed for educational institutions. It provides a dual-method approach to attendance (count-based and date-based) and automatically calculates exam eligibility based on institutional thresholds (75% general, 65% medical).

## ?? Key Features

### For Students
- **Real-time Analytics**: Instant view of attendance percentage and "classes to skip" analysis.
- **Dual Marking Methods**:
    - **Aggregate**: Quick entry of total conducted vs. attended classes.
    - **Date-Based**: Interactive calendar interface to mark specific dates, automatically excluding holidays.
- **Exam Eligibility Projections**: Calculates requirements for upcoming exams (CAT-1, CAT-2, FAT) and projected percentages.
- **Threshold Alerts**: Clear status indicators for 75% (General) and 65% (Medical/Cocurricular) eligibility.

### For Administrators
- **Academic Calendar Management**: Centralized control of term dates and exam schedules.
- **Holiday Management**: Global holiday lists that automatically update all student reports.
- **Course Administration**: Configure courses and monitor student engagement.
- **Automated Sync**: Student reports refresh automatically when administrators modify global settings or holiday lists.

---

## ?? Tech Stack

- **Backend**: Java 21, Spring Boot 4.0.3, Spring Security (JWT), Spring Data JPA.
- **Frontend**: Thymeleaf, HTML5/CSS3 (Modern Responsive UI), Bootstrap 5, Vanilla JavaScript.
- **Database**: PostgreSQL 16+.
- **DevOps**: Docker, Kubernetes (EKS), Terraform (IaC).
- **Monitoring**: Prometheus, Grafana, Spring Boot Actuator.
- **CI/CD**: GitHub Actions.

---

## ?? Project Structure

```text
src/main/java/com/deepak/Attendance/
+-- controller/    # Web endpoints and Thymeleaf page rendering
+-- service/       # Core business logic and eligibility formulas
+-- repository/    # Spring Data JPA interfaces for PostgreSQL
+-- entity/        # Data models (User, Course, Attendance, etc.)
+-- security/      # JWT authentication and Role-Based Access Control
+-- dto/           # Data Transfer Objects for requests/responses

infrastructure/
+-- terraform/     # AWS Infrastructure (EKS, RDS, VPC, ECR)
+-- k8s/           # Kubernetes manifests (Deployments, Services, Monitoring)
+-- docker/        # Containerization configurations
```

---

## ?? Setup & Installation

### Prerequisites
- **Java 21** (Eclipse Temurin recommended)
- **Maven 3.8+** (or use included ./mvnw)
- **PostgreSQL 15+**
- **Docker & Docker Compose** (for containerized runs)
- **AWS CLI & Terraform** (for cloud deployment)

### Local Development
1. **Database Setup**:
   Create a database named `attendance_db` in your PostgreSQL instance.

2. **Configuration**:
   Copy `src/main/resources/application.properties.example` to `application.properties` and update the credentials:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/attendance_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   jwt.secret=your_512bit_secret_key
   ```

3. **Run the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The application will be available at `http://localhost:8081`.

### Docker & Docker Compose
To run the entire stack (App + DB + Monitoring) locally:
```bash
docker-compose up --build
```

---

## ?? Deployment (AWS EKS)

The project includes a full IaC and CI/CD pipeline for AWS.

1. **Infrastructure**:
   ```bash
   cd terraform
   terraform init
   terraform apply
   ```
2. **Kubernetes Deployment**:
   ```bash
   # Update kubeconfig
   aws eks update-kubeconfig --region <region> --name <cluster-name>
   
   # Apply manifests
   kubectl apply -f k8s/db-secret.yaml
   kubectl apply -f k8s/monitoring.yaml
   kubectl apply -f k8s/deployment_and_service.yaml
   ```
3. **CI/CD**:
   GitHub Actions are configured to automatically build and push to ECR, update RDS hostnames, and perform a rolling update to EKS on every push to the main branch.

---

## ?? Monitoring

The system exports metrics via Micrometer for Prometheus/Grafana.

- **Endpoints**:
    - Prometheus: http://localhost:9090
    - Grafana: http://localhost:3000 (Credentials: admin/admin)
- **Dashboards**: Use Grafana Dashboard ID 4701 for detailed JVM Micrometer stats.
- **Actuator**: Access health and metrics at /actuator.

---

## ?? Security

- **Authentication**: JWT-based stateless authentication.
- **Authorization**: Role-Based Access Control (ROLE_STUDENT, ROLE_ADMIN).
- **Data Protection**: Secure password hashing and JWT token expiration.

---

## ?? Future Improvements
- [ ] Mobile application integration.
- [ ] Automated email alerts for low attendance.
- [ ] Integration with institutional LMS (Canvas/Moodle).
- [ ] Advanced biometric attendance integration.
