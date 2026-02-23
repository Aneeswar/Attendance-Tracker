# AttenTrack Deployment & Workflow Guide

This guide provides a comprehensive overview of the deployment lifecycle, infrastructure management, and CI/CD workflow for the AttenTrack Spring Boot application on AWS.

---

## 🚀 1. Deployment Architecture
*   **Infrastructure**: AWS VPC, EKS (Kubernetes), and RDS (PostgreSQL).
*   **Containerization**: Multi-stage Docker build using Eclipse Temurin 21.
*   **Orchestration**: Kubernetes Deployment with 2 replicas and a LoadBalancer service.
*   **CI/CD**: GitHub Actions for automated building, pushing to ECR, and deploying to EKS.

---

## 🛠️ 2. Infrastructure Management (Terraform)
The infrastructure is provisioned using modular Terraform code located in the `terraform/` directory.

### **Initial Setup & Provisioning**
```powershell
# Navigate to the terraform directory
cd terraform

# Initialize the Terraform workspace
terraform init

# Plan the infrastructure changes
terraform plan

# Apply the configuration to create the AWS resources
terraform apply -auto-approve
```

### **Resource Cleanup**
```powershell
# Destroy all managed infrastructure
terraform destroy -auto-approve
```

---

## 🔄 3. CI/CD Workflow (GitHub Actions)
The automated pipeline is defined in `.github/workflows/ecr-push.yml`.

### **Trigger**
Pushes to the `main` branch.

### **Pipeline Steps**
1.  **Checkout**: Pulls the latest code.
2.  **Configure AWS**: Authenticates via `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.
3.  **Login to ECR**: Authenticates the Docker CLI to your private ECR registry.
4.  **Build & Push**:
    *   Builds the Docker image tagged with the Git Commit SHA.
    *   Pushes the image to Amazon ECR.
5.  **Deploy to EKS**:
    *   Connects to the EKS cluster.
    *   **Dynamic Discovery**: Finds the RDS endpoint using `aws rds describe-db-instances`.
    *   **Kubectl Injection**: Sets the `DB_HOST` and updates the container `image` tag.
    *   **Rollout**: Monitors the rolling update until completion.

---

## 🚢 4. Manual Verification & Common Commands

### **Connectivity**
```powershell
# Update your local kubeconfig to interact with the cluster
aws eks update-kubeconfig --region ap-south-1 --name attentrack-cluster
```

### **Kubernetes Health Checks**
```powershell
# Check running pods
kubectl get pods

# Check service status and public LoadBalancer URL
kubectl get service attentrack-service

# View application logs (useful for debugging startup issues)
kubectl logs -l app=attentrack --tail=100 -f

# Describe a specific pod if it's stuck in "Pending" or "CrashLoopBackOff"
kubectl describe pod <pod-name>
```

### **Manual Secret Management**
Generate and apply the database credentials:
```powershell
# Create the secret manually (if not using CI/CD)
kubectl apply -f k8s/db-secret.yaml
```

---

## 🔧 5. Useful Administrative Commands

### **Scaling the Application**
```powershell
# Scale up to 3 replicas for higher traffic
kubectl scale deployment/attentrack-deployment --replicas=3
```

### **RDS Endpoint Lookup**
```powershell
# Manually find your RDS endpoint address
aws rds describe-db-instances --db-instance-identifier attentrack-cluster-db --query "DBInstances[0].Endpoint.Address" --output text
```

### **Forcing a Deployment Restart**
```powershell
# Restart all pods in the deployment
kubectl rollout restart deployment/attentrack-deployment
```

---

## 📋 6. Prerequisites for New Setup
1.  **IAM Permissions**: The GitHub Actions user must have `AmazonEC2ContainerRegistryPowerUser`, `AmazonEKSClusterPolicy`, and `AmazonRDSReadOnlyAccess`.
2.  **GitHub Secrets**:
    *   `AWS_ACCESS_KEY_ID`
    *   `AWS_SECRET_ACCESS_KEY`
3.  **Local Tools**: Terraform, AWS CLI, and Kubectl installed.
