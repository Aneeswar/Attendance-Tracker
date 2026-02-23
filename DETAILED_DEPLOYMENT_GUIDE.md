# ☁️ AttenTrack: Detailed Deployment Guide

This guide provides an in-depth explanation of the **Cloud, DevSecOps, and Infrastructure-as-Code** implementation for AttenTrack.

---

## 🐳 1. Containerization (Docker)

The application uses a **Multi-Stage Dockerfile** using **Eclipse Temurin 21** as the base image for both building and running.

### **Building Phase**
- Uses a full Maven library to compile the source code.
- Caches Maven dependencies in a layer to speed up subsequent builds.
- Outputs a single, executable fat JAR.

### **Running Phase**
- Uses a slimmed-down JRE image for security and size optimization.
- Runs as a **non-root user** to comply with security best practices.
- Configuration is injected via environment variables (`DB_HOST`, `DB_PORT`, etc.).

---

## 🏗️ 2. Infrastructure as Code (Terraform)

The entire AWS environment is defined in the `terraform/` directory, using modular AWS provider resources.

### **VPC Configuration**
- **Public Subnets**: Host the Load Balancer (ELB) and NAT Gateways.
- **Private Subnets**: Host the EKS Worker Nodes and RDS instances.
- **NAT Gateways**: Allow private nodes to fetch updates without direct internet exposure.

### **EKS Cluster (Elastic Kubernetes Service)**
- **Cluster Version**: 1.29+
- **Access Entries**: Configured to allow the `github-actions-ecr` IAM user to deploy to the cluster.
- **Managed Node Groups**: t3.medium instances that scale based on application load.

### **RDS Cluster (Relational Database Service)**
- **PostgreSQL 15/16**: Secured via Security Groups to only allow traffic from the EKS nodes.
- **Storage**: 20GB GP3 storage with auto-scaling capabilities.

---

## ☸️ 3. Orchestration (Kubernetes)

The workload is managed in the `k8s/` directory.

### **Deployment Object**
- **Replicas**: 2 (High Availability).
- **Rolling Update Strategy**: Ensures zero-downtime deployments.
- **Resource Limits**: Configured to prevent memory leaks from crashing nodes.

### **Service (LoadBalancer)**
- Exposes the application on a public URL.
- Automatically creates an AWS Classic/Network Load Balancer.

### **Secrets & ConfigMaps**
- Sensitive database credentials are stored in **Kubernetes Secrets** (`db-credentials`).
- Non-sensitive settings like the DB hostname are injected dynamically.

---

## 🏗️ 4. CI/CD Pipeline (GitHub Actions)

Located in `.github/workflows/ecr-push.yml`, this is the core automation engine.

### **The Automator Pattern**
1.  **Build**: Creates the immutable Docker image.
2.  **Push**: Uploads to Amazon ECR.
3.  **Endpoint Discovery**: 
    The workflow runs `aws rds describe-db-instances` to find the exact RDS hostname created by Terraform. This removes hardcoded dependencies.
4.  **Deployment**: 
    Uses `kubectl set image` to update the cluster without manual YAML edits.

---

## 🛠️ 5. Troubleshooting & Commands

### **AWS CLI Helpers**
- **List ENIs by Subnet**: `aws ec2 describe-network-interfaces --filters Name=subnet-id,Values=<subnet-id>`
- **Check IAM User**: `aws sts get-caller-identity`

### **Kubectl Helpers**
- **Check Rollout Status**: `kubectl rollout status deployment/attentrack-deployment`
- **Port-Forward to DB (Debug)**: `kubectl port-forward <pod-name> 5432:5432`

---

## 🔒 6. Security Hardening
- **Forced Deletion**: ECR and RDS instances are set with `force_delete` and `skip_final_snapshot` in Terraform (useful for dev/test environments).
- **Least Privilege IAM**: The GitHub Actions user is limited only to necessary ECR, EKS, and RDS operations.
