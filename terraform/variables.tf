variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
  default     = "attentrack-cluster"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "environment" {
  description = "Project environment"
  type        = string
  default     = "production"
}

variable "github_actions_iam_arn" {
  description = "IAM ARN of the user/role used in GitHub Actions"
  type        = string
  default     = "" # Empty by default
}

# --- Database Credentials ---
# SECURITY BEST PRACTICE: Never hardcode default values for production credentials.
# Pass these via environment variables (TF_VAR_db_password), a secure 
# terraform.tfvars file (ignored by git), or a secrets manager (AWS Secrets Manager).

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "attentrack"
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "dbadmin"
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true # This ensures the value is masked in the terminal output and logs
}
