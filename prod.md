
# Production Deployment Runbook

## 🚀 Quick Deploy Checklist

1. **One-time backend setup (if not done):**
  - Create S3 bucket and DynamoDB table for Terraform state (see below).
  - Migrate local state to remote backend.
2. **Run infra apply locally:**
  - `terraform -chdir=terraform apply`
3. **Set GitHub secrets:**
  - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `OCR_API_KEY`
4. **Push to main branch:**
  - CI/CD will build, push, and deploy all services.
5. **Verify rollout:**
  - `kubectl get deploy,pods,svc -n default`
  - `kubectl rollout status deployment/redis-deployment -n default --timeout=5m`
  - `kubectl rollout status deployment/ocr-deployment -n default --timeout=15m`
  - `kubectl rollout status deployment/attentrack-deployment -n default --timeout=15m`
6. **Check app service URL:**
  - `kubectl get svc attentrack-service -n default`

---

This document is the exact sequence to take AttenTrack to production.

## 1. Prerequisites

- AWS account access with permissions for EKS, RDS, ECR, IAM, S3, DynamoDB, Secrets Manager.
- Local tools installed:
  - `aws` CLI
  - `terraform` >= 1.5
  - `kubectl`
  - Docker (for local checks if needed)
- Repository checked out on your machine.
- GitHub repository with Actions enabled.

## 2. One-Time: Create Shared Terraform Backend

Terraform state must be shared between local runs and GitHub Actions.

### Backend resources used

- S3 bucket: `attentrack-tfstate-<AWS_ACCOUNT_ID>-ap-south-1`
- State key: `attendance-tracker/terraform.tfstate`
- DynamoDB lock table: `attentrack-terraform-locks`

### One-time setup commands (PowerShell)

```powershell
$region = "ap-south-1"
$account = (aws sts get-caller-identity --query Account --output text).Trim()
$bucket = "attentrack-tfstate-$account-$region"
$table = "attentrack-terraform-locks"

aws s3api create-bucket --bucket $bucket --create-bucket-configuration LocationConstraint=$region
aws s3api put-bucket-versioning --bucket $bucket --versioning-configuration Status=Enabled

$enc = @'{
  "Rules": [
    {
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }
  ]
}
'@
$encPath = Join-Path $env:TEMP "s3-backend-encryption.json"
Set-Content -Path $encPath -Value $enc -Encoding Ascii
aws s3api put-bucket-encryption --bucket $bucket --server-side-encryption-configuration file://$encPath

aws dynamodb create-table --table-name $table --attribute-definitions AttributeName=LockID,AttributeType=S --key-schema AttributeName=LockID,KeyType=HASH --billing-mode PAY_PER_REQUEST --region $region
```

If resources already exist, these create commands may fail. That is expected.

## 3. One-Time: Migrate Local Terraform State to Shared Backend

From repo root:

```powershell
$region = "ap-south-1"
$account = (aws sts get-caller-identity --query Account --output text).Trim()
$bucket = "attentrack-tfstate-$account-$region"
$key = "attendance-tracker/terraform.tfstate"
$table = "attentrack-terraform-locks"

terraform -chdir=terraform init -migrate-state -force-copy -backend-config="bucket=$bucket" -backend-config="key=$key" -backend-config="region=$region" -backend-config="dynamodb_table=$table" -backend-config="encrypt=true"
```

Verify backend + outputs:

```powershell
terraform -chdir=terraform output region
terraform -chdir=terraform output cluster_name
terraform -chdir=terraform output rds_endpoint
terraform -chdir=terraform output db_master_secret_arn
```

## 4. Provision / Update Infrastructure

Use local terminal for infra changes:

```powershell
terraform -chdir=terraform plan
terraform -chdir=terraform apply
```

After successful apply, required outputs are produced and consumed by CI/CD:

- `region`
- `cluster_name`
- `rds_endpoint`
- `db_name`
- `db_master_secret_arn`

## 5. GitHub Actions Configuration

## Required GitHub Secrets

Set in repository/environment secrets:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `OCR_API_KEY`

No DB username/password secret is required in GitHub.
The workflow fetches DB credentials directly from AWS Secrets Manager using `db_master_secret_arn` Terraform output.

## IAM notes for GitHub AWS principal

Minimum capabilities required:

- Read/write Terraform backend state object in S3.
- DynamoDB lock operations on `attentrack-terraform-locks`.
- ECR login and image push for `attentrack` and `attentrack-ocr` repositories.
- `eks:DescribeCluster` and cluster access entry authorization.
- `secretsmanager:GetSecretValue` for RDS master secret.
- `sts:GetCallerIdentity`.

## 6. Deployment Flow (Current)

On push to `main`, workflow `.github/workflows/ecr-push.yml` does:

1. Preflight shared backend and Terraform outputs.
2. Build/push app + OCR images to ECR.
3. Create/update runtime k8s secrets (`ocr-api-secret`, `db-credentials`).
4. Render app/OCR manifests with concrete values.
5. Apply Redis, OCR, then app manifests.
6. Rollout order and checks:
   - `redis-deployment`
   - `ocr-deployment`
   - `attentrack-deployment`
7. Verify deployment DB host matches Terraform `rds_endpoint`.

## 7. Redis in Production

Redis is deployed inside the cluster via:

- `k8s/redis-deployment-and-service.yaml`

App deployment uses:

- `SPRING_DATA_REDIS_HOST=redis`
- `SPRING_DATA_REDIS_PORT=6379`

This avoids fallback to localhost and prevents readiness/startup issues tied to Redis connectivity.

## 8. Manual Verification Commands

After Actions deploy:

```powershell
kubectl get deploy,pods,svc -n default
kubectl rollout status deployment/redis-deployment -n default --timeout=5m
kubectl rollout status deployment/ocr-deployment -n default --timeout=15m
kubectl rollout status deployment/attentrack-deployment -n default --timeout=15m
kubectl logs -n default -l app=attentrack --tail=200
```

Check app service hostname:

```powershell
kubectl get svc attentrack-service -n default
```

## 9. Standard Day-2 Process

For future updates:

1. If infra changed: run `terraform plan` and `terraform apply` locally.
2. Commit and push app/deploy code.
3. GitHub Actions deploys automatically from shared Terraform state.

## 10. Common Failure Patterns

- Backend access errors:
  - Ensure S3 bucket and DynamoDB lock table exist and IAM allows access.
- Missing Terraform outputs in CI:
  - Ensure local `terraform apply` was run against shared backend.
- App rollout stalls with readiness failures:
  - Check Redis deployment/service and app env values.
  - Review `kubectl describe pod` and `kubectl logs`.
